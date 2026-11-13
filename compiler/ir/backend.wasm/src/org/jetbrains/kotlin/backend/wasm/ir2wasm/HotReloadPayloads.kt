/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.wasm.ir.*
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation
import java.util.TreeMap

/**
 * Hot-reload payload versioning — CL defclass semantics on wasm GC. The
 * previous-state store is the IMAGE REGISTRY (see readInjectedRegistry):
 * a class whose field set changes mints a new payload VERSION, and
 * already-allocated instances migrate lazily, in place, on first
 * new-code field access. Semantics: addition = subtype of the largest
 * prefix-compatible ancestor (old code upcasts and keeps reading
 * surviving fields); deletion/type change = sibling version, and the
 * version cast on a migrated instance is the trampoline; redefinition
 * back to a previous shape reuses that version, so instances holding
 * later versions heal with no migration. Version keys are synthetic
 * signatures derived from the class reference key, so the whole history
 * re-emits byte-identically and cross-module canonicalization keeps one
 * type universe. The version decision is a pure function of the class's
 * fields and the registry, so type generation, field access and
 * allocation agree regardless of file order; resolved plans are cached
 * per compilation in [WasmBackendContext.hotReloadPayloadPlans].
 */
object HotReloadPayloads {

    // ---------------------------------------------------------------- keys

    private const val SIGNATURE_PACKAGE = "hot.reload.payload"
    private val VERSION_SUFFIX = Regex("""\.#v(\d+)$""")
    private const val CLASS_PAYLOAD_FIELD_ID = 3 // vtable, itable, rtti, payload

    fun payloadVersionSignature(classKey: String, version: Int): IdSignature {
        val base = "payload<\$$classKey>"
        val name = if (version <= 1) base else "$base.#v$version"
        return IdSignature.CommonSignature(SIGNATURE_PACKAGE, name, null, 0, null)
    }

    /** Signature of the per-class payload stepper `(ref null $Cell) ->
     *  (ref $payload<current>)`, which lazily migrates the cell and returns
     *  its current-version payload. */
    fun stepperSignature(classKey: String, currentVersion: Int): IdSignature =
        IdSignature.CommonSignature(
            SIGNATURE_PACKAGE,
            "payloadOf<\$$classKey>.#v$currentVersion",
            null, 0, null,
        )

    private fun migrateFromSignature(classKey: String, from: Int, to: Int): IdSignature =
        IdSignature.CommonSignature(
            SIGNATURE_PACKAGE,
            "migrateFrom<\$$classKey>.#v$from>#v$to",
            null, 0, null,
        )

    // ------------------------------------------------- object adoption

    /** Host module of the synthesized object-adoption imports. */
    const val HOT_ADOPT_MODULE = "host"

    /**
     * Prefix of the synthesized host import that gives `object` (and
     * companion) declarations defvar semantics across redefinitions (see
     * WasmHotReloadObjectAdoptionLowering). The import name is
     * `<object name>#<referenceKey.hashCode() in hex>`: the hashCode is
     * the one invariant that survives BOTH the klib round trip and edits
     * of the declaring file (the slot sweep's `#<suffix>` disambiguator
     * rides on the same property), so the host finds the previous live
     * instance's `__hot_entry$…#<suffix>` export by it — or supplies null
     * when none exists (cold boot, a brand-new object). Full identity
     * tags and fq names are NOT stable across edits; the suffix is.
     */
    const val HOT_ADOPT_PREFIX = "__hot_adopt$"

    /** The import name of [declaration] when it is a synthesized adopt
     *  function, else null. */
    fun hotAdoptImportName(declaration: IrSimpleFunction): String? =
        declaration.name.asString().takeIf { it.startsWith(HOT_ADOPT_PREFIX) }

    fun hotAdoptFunctionName(objectName: String, referenceKey: IdSignature): String =
        HOT_ADOPT_PREFIX + objectName + "#" + Integer.toHexString(referenceKey.hashCode())

    // ------------------------------------------------------- the decision

    /** (class reference key string, version, struct) triples for the
     *  payload entries in a gcTypes map — the shared parse for the injected
     *  registry and any other payload-version source. */
    fun parsePayloadEntries(gcTypes: Map<IdSignature, WasmTypeDeclaration>): List<Triple<String, Int, WasmStructDeclaration>> {
        val result = mutableListOf<Triple<String, Int, WasmStructDeclaration>>()
        for ([signature, declaration] in gcTypes) {
            if (declaration !is WasmStructDeclaration) continue
            if (signature !is IdSignature.CommonSignature) continue
            if (signature.packageFqName != SIGNATURE_PACKAGE) continue
            // entry name: "payload<$classKey>" for v1, "payload<$classKey>.#vN"
            // for later versions — the version suffix sits AFTER the '>'.
            val stripped = signature.declarationFqName.removePrefix("payload<\$")
            val match = VERSION_SUFFIX.find(stripped)
            val core = match?.let { stripped.removeRange(it.range) } ?: stripped
            if (!core.endsWith(">")) continue
            result.add(
                Triple(
                    core.removeSuffix(">"),
                    match?.let { it.groupValues[1].toInt() } ?: 1,
                    declaration,
                )
            )
        }
        return result
    }

    // -------------------------------------------------- the image registry

    /**
     * Internal transport for the previous module's registry between the IC
     * compile (the only place the injected source can be read) and the
     * output phase (where the section is emitted) — an internal
     * configuration key, NOT a CLI option. Delta emission makes the merge
     * it enables load-bearing: files the IC left clean contribute no
     * gcTypes to the emitted module, so their classes' payload history
     * reaches the new section only by merging the previous table in —
     * without it the host's ferry would lose history and a later edit of
     * a clean file would break its version chain.
     */
    val HOT_RELOAD_PREVIOUS_REGISTRY: CompilerConfigurationKey<Map<IdSignature, WasmTypeDeclaration>> =
        CompilerConfigurationKey.create("HOT_RELOAD_PREVIOUS_REGISTRY")

    /**
     * The registry the previous module embedded as a wasm custom section:
     * the payload version entries for every class that has ever bumped a
     * version (classes still on v1 are recomputable from current IR, so
     * they are not recorded — keeps the section proportional to the
     * classes that actually evolved). Serialized with the klib serializer's
     * own types-fragment codec, so byte-identical re-emission of the
     * stored structs is trivial.
     */
    const val REGISTRY_SECTION_NAME = "hot-reload-registry"

    /** The name of the hidden source object the host injects to ferry the
     *  registry back into the next compilation (const REGISTRY = base64). */
    const val REGISTRY_OBJECT_NAME = "__hotRegistry"
    const val REGISTRY_PROPERTY_NAME = "REGISTRY"

    /**
     * The COMPLETE payload-version table the module declares: every
     * class's current version entry plus every historical version of
     * bumped classes. Completeness is load-bearing — the registry is the
     * compiler's ONLY previous-state input (the IC-cache channel is gone),
     * and a class's FIRST bump needs the previous v1 shape, which an
     * unbumped class's current entry provides. Stdlib entries ride along
     * (constant across generations); scoping the section to the compiled
     * module's own classes is a listed size follow-up.
     */
    fun collectRegistryEntries(gcTypes: Map<IdSignature, WasmTypeDeclaration>): Map<IdSignature, WasmTypeDeclaration> =
        gcTypes.filterTo(linkedMapOf()) { [signature, declaration] ->
            declaration is WasmStructDeclaration &&
                    signature is IdSignature.CommonSignature &&
                    signature.packageFqName == SIGNATURE_PACKAGE &&
                    signature.declarationFqName.startsWith("payload<$")
        }

    fun serializeRegistry(registry: Map<IdSignature, WasmTypeDeclaration>): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val serializer = org.jetbrains.kotlin.backend.wasm.serialization.WasmSerializer(output)
        serializer.serializeCompiledTypes(WasmCompiledTypesFileFragment(definedGcTypes = registry.toMutableMap()))
        return output.toByteArray()
    }

    fun deserializeRegistry(bytes: ByteArray): Map<IdSignature, WasmTypeDeclaration> =
        bytes.inputStream().use {
            org.jetbrains.kotlin.backend.wasm.serialization.WasmDeserializer(inputStream = it, skipLocalNames = false)
                .deserializeCompiledTypesFragment()
                .definedGcTypes
        }

    /**
     * Reads the registry the HOST ferried back into this compilation as the
     * hidden source object `[REGISTRY_OBJECT_NAME] { const val [REGISTRY_PROPERTY_NAME] = "<base64>" }`
     * — the driver/dev task reads the previous module's custom section from
     * the LIVE image and injects it, making the previous-state store
     * image-derived rather than cache-derived. Scanned before any lowering,
     * so the const initializer is intact.
     */
    fun readInjectedRegistry(allModules: Collection<IrModuleFragment>): Map<IdSignature, WasmTypeDeclaration> {
        for (module in allModules) {
            for (file in module.files) {
                for (declaration in file.declarations) {
                    if (declaration !is IrClass || declaration.name.asString() != REGISTRY_OBJECT_NAME) continue
                    for (member in declaration.declarations) {
                        if (member !is IrProperty || member.name.asString() != REGISTRY_PROPERTY_NAME) continue
                        val const = member.backingField?.initializer?.expression as? IrConst ?: continue
                        val text = const.value as? String ?: continue
                        return try {
                            deserializeRegistry(java.util.Base64.getDecoder().decode(text))
                        } catch (e: IllegalArgumentException) {
                            // a malformed registry must never kill the compile;
                            // the IC channel still provides history if it can
                            emptyMap()
                        }
                    }
                }
            }
        }
        return emptyMap()
    }

    // ------------------------------------------------------- the decision

    class PayloadPlan(
        val irClass: IrClassSymbol,
        val classKey: String,
        /** the version field access casts to / allocation news with */
        val currentVersion: Int,
        /** struct to emit for the current version (a stored one when reused) */
        val currentStruct: WasmStructDeclaration,
        /** every stored version (ascending) — re-emitted byte-identically */
        val history: List<Pair<Int, WasmStructDeclaration>>,
        /** stored versions that fail a cast to [currentVersion]; the stepper
         *  migrates instances holding them */
        val migrateFromVersions: List<Int>,
    )

    /**
     * The plan for [irClass], cache-first: the declaring file's type
     * generation and any file's field-access codegen must agree, and the
     * decision is a pure function, so file ordering does not matter.
     * [keyOf] computes a class's reference key string (the computation is
     * protected in the codegen context, which every caller holds).
     */
    fun payloadPlan(
        irClass: IrClass,
        keyOf: (IrClassSymbol) -> String,
        backendContext: WasmBackendContext,
        wasmModuleTypeTransformer: WasmModuleTypeTransformer,
    ): PayloadPlan =
        backendContext.hotReloadPayloadPlans.getOrPut(keyOf(irClass.symbol)) {
            resolvePayloadPlan(irClass, keyOf, backendContext, wasmModuleTypeTransformer)
        }

    private fun currentPayloadVersion(
        irClass: IrClass,
        keyOf: (IrClassSymbol) -> String,
        backendContext: WasmBackendContext,
        transformer: WasmModuleTypeTransformer,
    ): Int = payloadPlan(irClass, keyOf, backendContext, transformer).currentVersion

    private fun resolvePayloadPlan(
        irClass: IrClass,
        keyOf: (IrClassSymbol) -> String,
        backendContext: WasmBackendContext,
        transformer: WasmModuleTypeTransformer,
    ): PayloadPlan {
        val irBuiltIns = backendContext.irBuiltIns
        val symbol = irClass.symbol
        val classKey = keyOf(symbol)
        val historyMap = backendContext.hotReloadPayloadHistory[classKey] ?: TreeMap()
        val flatFields = flatPayloadFields(irClass, irBuiltIns, transformer)
        val history = historyMap.entries.map { it.key to it.value }

        val currentVersion: Int
        val currentStruct: WasmStructDeclaration

        if (history.isEmpty()) {
            currentVersion = 1
            currentStruct = freshStruct(irClass, keyOf, flatFields, null, backendContext, transformer)
        } else {
            val newest = historyMap.lastKey()
            val contentMatch = (newest downTo 1).firstOrNull { v ->
                fieldsEqual(historyMap[v]!!.fields, flatFields)
            }
            if (contentMatch != null) {
                currentVersion = contentMatch
                currentStruct = historyMap[contentMatch]!!
            } else {
                val anchor = (newest downTo 1).firstOrNull { v ->
                    isPrefixOf(historyMap[v]!!.fields, flatFields)
                }
                currentVersion = newest + 1
                currentStruct = freshStruct(irClass, keyOf, flatFields, anchor, backendContext, transformer)
            }
        }

        // A held version passes the field-access cast iff the current
        // version appears in its supertype chain; everything else migrates.
        val migrateFromVersions = history.map { it.first }.filter { v ->
            v != currentVersion && !isAncestorOf(currentVersion, v, historyMap, classKey)
        }

        return PayloadPlan(symbol, classKey, currentVersion, currentStruct, history, migrateFromVersions)
    }

    private fun isAncestorOf(
        currentVersion: Int,
        heldVersion: Int,
        historyMap: TreeMap<Int, WasmStructDeclaration>,
        classKey: String,
    ): Boolean {
        val currentKey = payloadVersionSignature(classKey, currentVersion)
        var superType = historyMap[heldVersion]?.superType
        while (superType is GcHeapTypeSymbol) {
            if (superType.type == currentKey) return true
            // walk further within this class's own version chain
            superType = historyMap.entries
                .firstOrNull { payloadVersionSignature(classKey, it.key) == superType.type }
                ?.value?.superType
        }
        return false
    }

    private fun flatPayloadFields(
        irClass: IrClass,
        irBuiltIns: org.jetbrains.kotlin.ir.IrBuiltIns,
        transformer: WasmModuleTypeTransformer,
    ): List<WasmStructFieldDeclaration> =
        irClass.allFields(irBuiltIns).map {
            WasmStructFieldDeclaration(
                name = it.name.toString(),
                type = transformer.transformFieldType(it.type),
                isMutable = true,
            )
        }

    private fun freshStruct(
        irClass: IrClass,
        keyOf: (IrClassSymbol) -> String,
        flatFields: List<WasmStructFieldDeclaration>,
        anchorVersion: Int?,
        backendContext: WasmBackendContext,
        transformer: WasmModuleTypeTransformer,
    ): WasmStructDeclaration {
        val superHeap = if (anchorVersion != null) {
            // a new version chained onto the anchor version of THIS class
            GcHeapTypeSymbol(payloadVersionSignature(keyOf(irClass.symbol), anchorVersion))
        } else {
            // sibling / first version: subtype the superclass's CURRENT payload
            irClass.getSuperClass(backendContext.irBuiltIns)?.let { superClass ->
                GcHeapTypeSymbol(
                    payloadVersionSignature(keyOf(superClass.symbol), currentPayloadVersion(superClass, keyOf, backendContext, transformer))
                )
            }
        }
        return WasmStructDeclaration(
            name = "${irClass.fqNameWhenAvailable.toString()}\$payload",
            fields = flatFields,
            superType = superHeap,
            isFinal = false,
        )
    }

    private fun fieldsEqual(a: List<WasmStructFieldDeclaration>, b: List<WasmStructFieldDeclaration>): Boolean {
        if (a.size != b.size) return false
        return a.zip(b).all { [x, y] -> x.name == y.name && x.type == y.type && x.isMutable == y.isMutable }
    }

    private fun isPrefixOf(prefix: List<WasmStructFieldDeclaration>, full: List<WasmStructFieldDeclaration>): Boolean {
        if (prefix.size > full.size) return false
        return fieldsEqual(prefix, full.subList(0, prefix.size))
    }

    // ------------------------------------------------ function generation

    /** Generates the per-class stepper + migrateFrom functions for a
     * plan whose held versions fail the current-version cast; field
     * access lowers to a stepper CALL (migrate in place, return the
     * current payload). Classes with no migration-requiring history keep
     * the bare extract+cast shape. */
    fun generatePayloadFunctions(
        plan: PayloadPlan,
        backendContext: WasmBackendContext,
        typeCodegenContext: WasmTypeCodegenContext,
        declarationCodegenContext: WasmDeclarationCodegenContext,
    ) {
        if (plan.migrateFromVersions.isEmpty()) return
        val symbol = plan.irClass
        val location = SourceLocation.NoLocation("Hot reload payload migration")

        val cellGcType = typeCodegenContext.referenceGcType(symbol)
        val cellHeap = typeCodegenContext.referenceHeapType(symbol)
        val currentKey = payloadVersionSignature(plan.classKey, plan.currentVersion)
        val currentHeap = GcHeapTypeSymbol(currentKey)
        val rootPayloadHeap = typeCodegenContext.referencePayloadHeapType(backendContext.irBuiltIns.anyClass)
        val cellParam = WasmLocal(0, "cell", WasmRefNullType(cellHeap), true)

        // ---- migrateFrom v -> current: read surviving fields by name out of
        // the old version, fill the rest with type defaults, swap the cell's
        // payload, return it.
        for (fromVersion in plan.migrateFromVersions) {
            val fromStruct = plan.history.first { it.first == fromVersion }.second
            val fromKey = payloadVersionSignature(plan.classKey, fromVersion)
            val fromHeap = GcHeapTypeSymbol(fromKey)

            val pLocal = WasmLocal(1, "p", WasmRefNullType(fromHeap), false)
            val npLocal = WasmLocal(2, "np", WasmRefType(currentHeap), false)

            val body = mutableListOf<WasmInstr>()
            val builder = WasmExpressionBuilder(expression = body)
            builder.buildGetLocal(cellParam, location)
            builder.buildStructGet(cellGcType, CLASS_PAYLOAD_FIELD_ID, location)
            builder.buildRefCastStatic(fromHeap, location) // guaranteed by the stepper's test
            builder.buildSetLocal(pLocal, location)
            for (field in plan.currentStruct.fields) {
                val sourceIdx = fromStruct.fields.indexOfFirst { it.name == field.name && it.type == field.type }
                if (sourceIdx >= 0) {
                    builder.buildGetLocal(pLocal, location)
                    builder.buildStructGet(GcTypeSymbol(fromKey), sourceIdx, location)
                } else {
                    generateDefaultInitializerForType(field.type, builder)
                }
            }
            builder.buildStructNew(GcTypeSymbol(currentKey), location)
            builder.buildSetLocal(npLocal, location)
            builder.buildGetLocal(cellParam, location)
            builder.buildGetLocal(npLocal, location)
            builder.buildStructSet(cellGcType, CLASS_PAYLOAD_FIELD_ID, location)
            builder.buildGetLocal(npLocal, location)

            declarationCodegenContext.defineSyntheticFunction(
                migrateFromSignature(plan.classKey, fromVersion, plan.currentVersion),
                WasmFunction.Defined(
                    name = "__hot_migrate_from$" + symbol.owner.name + "#v$fromVersion",
                    type = typeCodegenContext.referenceWasmFunctionHeapType(
                        WasmFunctionType(listOf(WasmRefNullType(cellHeap)), listOf(WasmRefType(currentHeap)))
                    ),
                    locals = mutableListOf(cellParam, pLocal, npLocal),
                    instructions = body,
                ),
            )
        }

        // ---- the stepper: fast path when already current, else find the
        // held version and migrate. Nested result-typed ifs.
        val pLocal = WasmLocal(1, "p", WasmRefNullType(rootPayloadHeap), false)

        fun buildMigrationArms(builder: WasmExpressionBuilder, versions: List<Int>) {
            if (versions.isEmpty()) {
                builder.buildUnreachable(location)
                return
            }
            val v = versions.first()
            builder.buildGetLocal(pLocal, location)
            builder.buildRefTestStatic(GcHeapTypeSymbol(payloadVersionSignature(plan.classKey, v)), location)
            builder.buildIf(null, WasmRefType(currentHeap))
            builder.buildGetLocal(cellParam, location)
            builder.buildCall(FuncSymbol(migrateFromSignature(plan.classKey, v, plan.currentVersion)), location)
            builder.buildElse()
            buildMigrationArms(builder, versions.drop(1))
            builder.buildEnd()
        }

        val stepperBody = mutableListOf<WasmInstr>()
        val stepperBuilder = WasmExpressionBuilder(expression = stepperBody)
        stepperBuilder.buildGetLocal(cellParam, location)
        stepperBuilder.buildStructGet(cellGcType, CLASS_PAYLOAD_FIELD_ID, location) // traps on a null cell, like the bare extract
        stepperBuilder.buildSetLocal(pLocal, location)
        stepperBuilder.buildGetLocal(pLocal, location)
        stepperBuilder.buildRefTestStatic(currentHeap, location)
        stepperBuilder.buildIf(null, WasmRefType(currentHeap))
        stepperBuilder.buildGetLocal(pLocal, location)
        stepperBuilder.buildRefCastStatic(currentHeap, location)
        stepperBuilder.buildElse()
        buildMigrationArms(stepperBuilder, plan.migrateFromVersions)
        stepperBuilder.buildEnd()

        declarationCodegenContext.defineSyntheticFunction(
            stepperSignature(plan.classKey, plan.currentVersion),
            WasmFunction.Defined(
                name = "__hot_payload$" + symbol.owner.name + "#v" + plan.currentVersion,
                type = typeCodegenContext.referenceWasmFunctionHeapType(
                    WasmFunctionType(listOf(WasmRefNullType(cellHeap)), listOf(WasmRefType(currentHeap)))
                ),
                locals = mutableListOf(cellParam, pLocal),
                instructions = stepperBody,
            ),
        )
    }
}
