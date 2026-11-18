/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.common.serialization.cityHash64
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.erasedUpperBound
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getInlineClassBackingField
import org.jetbrains.kotlin.backend.wasm.utils.isAbstractOrSealed
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.backend.wasm.lower.WasmSuspendLambdaMergingLowering
import org.jetbrains.kotlin.ir.backend.js.lower.WebCallableReferenceLowering
import org.jetbrains.kotlin.wasm.ir.*
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation

/**
 * Hot-reload class layouts — CL defclass semantics on wasm GC, the
 * SBCL model translated. A class whose field set changes mints a
 * payload of a NEW SHAPE; every payload's `$layout` field points directly
 * at the LAYOUT object it was shaped by — SBCL's wrapper with the
 * indirection removed (class identity and slots fused), pointer
 * identity. Field access tests the payload against
 * the class's current layout's TYPE — the type system is the check:
 * same-shape instances read inline at full speed; a shape change
 * falls to the IMAGE's generic machinery (update-instance: copy
 * same-named same-tagged slots in place; slot-value for OLD code,
 * where a deleted or retyped slot throws the honest catchable
 * unbound-slot).
 *
 * The layout is a pure function of the class's current IR, so type
 * generation, field access and allocation agree regardless of file
 * order; plans are cached in [WasmBackendContext.hotReloadPayloadPlans].
 */
object HotReloadPayloads {

    // ---------------------------------------------------------------- keys

    private const val SIGNATURE_PACKAGE = "hot.reload.payload"
    const val CLASS_PAYLOAD_FIELD_ID = 3 // vtable, itable, rtti, payload
    const val PAYLOAD_LAYOUT_FIELD_ID = 1 // the ROOT payload's own fields: _hashCode, $layout

    /** The ROOT payload's key — kotlin.Any's payload struct
     *  ({_hashCode: mut i32, $layout: (ref null $HotLayout)}). FIXED:
     *  every payload is a subtype of it and the cell's mutable payload
     *  field is invariantly typed by it, so the generic machinery reads
     *  any instance's own layout through it — one struct.get. */
    val PAYLOAD_ROOT_TYPE: IdSignature = synthetic("payloadRoot")

    /** The payload-type key — a PURE FUNCTION OF THE CLASS (one
     *  compilation holds exactly one shape per class). Identical
     *  shapes unify through the ENGINE's content canonicalization,
     *  not the key; changed shapes meet only across instances, where
     *  migration handles them (a delta redefining a BASE-declared
     *  class collides — a loud link error, the documented limit). */
    fun payloadLayoutSignature(classKey: String): IdSignature =
        IdSignature.CommonSignature(SIGNATURE_PACKAGE, "payload<\$$classKey>", null, 0, null)

    /** The reader/writer/allocate signatures — one per (class, slot).
     *  Readers and writers share one erased wasm signature each (see
     *  [SLOT_READER_WASM_TYPE]) so the stdlib's call primitives can
     *  dispatch every layout's closures through one static call_ref. */
    fun slotReaderSignature(classKey: String, field: IrField): IdSignature =
        IdSignature.CommonSignature(SIGNATURE_PACKAGE, "slotReader<\$$classKey:${field.name.asString()}>", null, 0, null)

    fun slotWriterSignature(classKey: String, field: IrField): IdSignature =
        IdSignature.CommonSignature(SIGNATURE_PACKAGE, "slotWriter<\$$classKey:${field.name.asString()}>", null, 0, null)

    fun slotAllocateSignature(classKey: String): IdSignature =
        IdSignature.CommonSignature(SIGNATURE_PACKAGE, "slotAllocate<\$$classKey>", null, 0, null)

    /** The image's identity for the class — the layout table's key half
     *  (and the SAME content hash the RTTI carries as klassId, so the
     *  generic machinery can derive an instance's concrete class from
     *  its cell). */
    fun classIdOf(classKey: String): Long = cityHash64(classKey.toByteArray()).toLong()

    // ------------------------------------------------- the erased slot ABI

    /** The shared erased accessor signatures: every layout's readers,
     *  writers and allocate carry these, so the image's migration code
     *  dispatches through one static call_ref per kind — the type
     *  knowledge lives in the concrete casts inside each closure. */
    val SLOT_READER_WASM_TYPE = WasmFunctionType(listOf(WasmAnyRef), listOf(WasmAnyRef))
    val SLOT_WRITER_WASM_TYPE = WasmFunctionType(listOf(WasmAnyRef, WasmAnyRef), emptyList())
    val SLOT_ALLOCATE_WASM_TYPE = WasmFunctionType(emptyList(), listOf(WasmAnyRef))

    // ------------------------------------------------------------- the plan

    class PayloadLayoutPlan(
        val irClass: IrClassSymbol,
        val classKey: String,
        /** the image's class identity (== the RTTI klassId) */
        val classId: Long,
        /** the payload struct's key ([PAYLOAD_ROOT_TYPE] for Any) */
        val structKey: IdSignature,
        val currentStruct: WasmStructDeclaration,
        /** the class's SLOTS (its real fields — everything but Any's
         *  _hashCode), in [IrClass.allFields] order; slot j lives at
         *  [payloadFieldIndexOf]`(j + 1)` in [currentStruct] */
        val slots: List<IrField>,
    )

    /**
     * The plan for [irClass], cache-first: the declaring file's type
     * generation and any file's field-access codegen must agree, and the
     * decision is a pure function, so file ordering does not matter.
     * [keyOf] computes a class's reference key string.
     */
    fun payloadLayoutPlan(
        irClass: IrClass,
        keyOf: (IrClassSymbol) -> String,
        backendContext: WasmBackendContext,
        transformer: WasmModuleTypeTransformer,
    ): PayloadLayoutPlan =
        backendContext.hotReloadPayloadPlans.getOrPut(keyOf(irClass.symbol)) {
            resolveLayoutPlan(irClass, keyOf, backendContext, transformer)
        }

    private fun resolveLayoutPlan(
        irClass: IrClass,
        keyOf: (IrClassSymbol) -> String,
        backendContext: WasmBackendContext,
        transformer: WasmModuleTypeTransformer,
    ): PayloadLayoutPlan {
        val symbol = irClass.symbol
        val classKey = keyOf(symbol)
        // the layout class's cell type key — the $layout field's type
        val layoutCellKey = backendContext.idSignatureRetriever
            .declarationSignature(backendContext.wasmSymbols.hotLayoutClass.owner)!!
        val fields = flatPayloadFields(irClass, backendContext.irBuiltIns, transformer)
        val slots = irClass.allFields(backendContext.irBuiltIns).drop(1) // everything past Any's _hashCode
        val superHeap = irClass.getSuperClass(backendContext.irBuiltIns)?.let { superClass ->
            GcHeapTypeSymbol(payloadLayoutPlan(superClass, keyOf, backendContext, transformer).structKey)
        }
        val isRoot = irClass.symbol == backendContext.irBuiltIns.anyClass
        val currentStruct = WasmStructDeclaration(
            name = (irClass.fqNameWhenAvailable?.toString() ?: "<anonymous>") + "\$payload",
            fields = fields.subList(0, 1) +
                    // the layout pointer; restated in every payload, so an
                    // instance's own layout is one root-typed struct.get
                    WasmStructFieldDeclaration(
                        "\$layout",
                        WasmRefNullType(GcHeapTypeSymbol(layoutCellKey)),
                        isMutable = false,
                    ) +
                    fields.subList(1, fields.size),
            superType = superHeap,
            isFinal = false,
        )
        return PayloadLayoutPlan(
            irClass = symbol,
            classKey = classKey,
            classId = classIdOf(classKey),
            structKey = if (isRoot) PAYLOAD_ROOT_TYPE else payloadLayoutSignature(classKey),
            currentStruct = currentStruct,
            slots = slots,
        )
    }

    /** The payload's own fields: the class's FLAT fields (`allFields` —
     *  Any's `_hashCode` first), mutable, transformer-rendered. The
     *  `$layout` is injected at index 1 by [resolveLayoutPlan]; every
     *  payload restates the root's fields, as the wasm text requires
     *  of subtypes. */
    private fun flatPayloadFields(
        irClass: IrClass,
        irBuiltIns: IrBuiltIns,
        transformer: WasmModuleTypeTransformer,
    ): List<WasmStructFieldDeclaration> =
        irClass.allFields(irBuiltIns).map {
            WasmStructFieldDeclaration(
                name = it.name.toString(),
                type = transformer.transformFieldType(it.type),
                isMutable = true,
            )
        }

    /** The slot's NAME identity within its layout. */
    fun slotNameTag(field: IrField): Long = cityHash64(field.name.toString().toByteArray()).toLong()

    /** The slot's TYPE tag — migration's copy-vs-default decision
     *  (same name and same tag copies; a retyped slot defaults). The
     *  tag IS the class identity the backend already uses —
     *  [WasmLinkerDataCodegenContext.referenceTypeId], the same id the
     *  RTTI carries as klassId — of the field type's classifier class
     *  (primitives are classes too, so Boolean and Byte are distinct
     *  despite sharing the i8 payload kind; a bare type parameter tags
     *  as kotlin.Any). A TYPED funcref slot tags by its wasm SIGNATURE
     *  instead — the marker class alone would let a signature retype
     *  pass the check and trap the writer's cast mid-migration.
     *  Tag-equal means cast-compatible by construction: the tag names
     *  the very class (or signature) the writer's unbox cast targets.
     *  Low bit: nullability. */
    fun slotTypeTag(field: IrField, wasmType: WasmType, keyOf: (IrClassSymbol) -> String, irBuiltIns: IrBuiltIns): Long {
        val funHeap = when (wasmType) {
            is WasmRefType -> wasmType.heapType
            is WasmRefNullType -> wasmType.heapType
            else -> null
        }
        if (funHeap is FunctionHeapTypeSymbol) {
            return (classIdOf(funHeap.type.toString()) shl 1) or 1L
        }
        val type = field.type as? IrSimpleType
        val classifier = type?.classifier
        val classId = classIdOf(keyOf(classifier as? IrClassSymbol ?: irBuiltIns.anyClass))
        return (classId shl 1) or (if (type?.isMarkedNullable() == true) 1L else 0L)
    }

    /** The field's index in the class's payload struct: the FLAT
     *  ClassMetadata index, +1 past `_hashCode` (the `$layout` sits at
     *  index 1 of every payload); `_hashCode` itself is index 0. */
    fun payloadFieldIndexOf(flatIndex: Int): Int = if (flatIndex == 0) 0 else flatIndex + 1

    /** Whether the class gets a LAYOUT (and a layout global): CONCRETE
     *  classes only — a layout is keyed by the CONCRETE class an
     *  instance's RTTI names, and the machinery origins (callable
     *  references, merged suspend lambdas) are never redefined.
     *  Classes without layouts stamp a null `$layout`. */
    fun hasLayout(irClass: IrClass): Boolean =
        !irClass.isAbstractOrSealed &&
                irClass.origin != WebCallableReferenceLowering.FUNCTION_REFERENCE_IMPL &&
                irClass.origin != WasmSuspendLambdaMergingLowering.SUSPEND_LAMBDA_MERGING_CLASS

    /** The slot's VALUE representation: packed payload fields (i8/i16 —
     *  Boolean/Byte/Char/Short slots) move as i32 on the stack, so block
     *  result types and set-site temps use the unpacked stand-in (the
     *  struct field itself stays packed; its struct.get/struct.set take
     *  i32 either way). */
    fun slotValueType(fieldType: WasmType): WasmType = when (fieldType) {
        WasmI8, WasmI16 -> WasmI32
        else -> fieldType
    }

    /** Whether the wasm type is a function reference — funcrefs are
     *  not anyref subtypes (the func and any hierarchies are
     *  disjoint), so slot values of this shape cross the erased
     *  channel inside the stdlib's box (see [boxSlotValue] and
     *  [unboxSlotValue]). */
    fun isFuncrefSlot(fieldType: WasmType): Boolean = when (fieldType) {
        WasmFuncRef -> true
        is WasmRefType -> isFuncrefHeap(fieldType.heapType)
        is WasmRefNullType -> isFuncrefHeap(fieldType.heapType)
        else -> false
    }

    /** Both spellings: the ABSTRACT func heap (a Kotlin `funcref` field
     *  transforms to (ref null func)) and the typed function heaps. */
    private fun isFuncrefHeap(heapType: WasmHeapType): Boolean =
        heapType is FunctionHeapTypeSymbol || heapType == WasmHeapType.Simple.Func

    // ------------------------------------------------ function generation

    /** Generates the class's LAYOUT functions — per-slot readers and
     *  writers (erased signatures, concrete casts inside) and the
     *  allocate closure (a fresh payload with type defaults) — and
     *  records the layout binding for the link's init. */
    fun generateLayoutFunctions(
        plan: PayloadLayoutPlan,
        backendContext: WasmBackendContext,
        typeCodegenContext: WasmTypeCodegenContext,
        declarationCodegenContext: WasmDeclarationCodegenContext,
        linkerDataContext: WasmLinkerDataCodegenContext,
        transformer: WasmModuleTypeTransformer,
    ) {
        val location = SourceLocation.NoLocation("Hot reload layout functions")
        // the identity channel: the layout table's key and the RTTI
        // klassId are the SAME content hash — assert it, so a drift on
        // either side fails the compile instead of the runtime
        check(plan.classId == linkerDataContext.referenceTypeId(plan.irClass)) {
            "hot reload: classId drift for ${plan.classKey}"
        }
        // through the tracked context: the payload types and the layout
        // class's CELL type must land in the module's referenced set or
        // the IC projection drops them
        typeCodegenContext.referenceHeapType(backendContext.wasmSymbols.hotLayoutClass)
        val payloadGcType = typeCodegenContext.referencePayloadLayoutGcType(plan.irClass)
        val payloadHeap = typeCodegenContext.referencePayloadLayoutHeapType(plan.irClass)
        val readerSig = typeCodegenContext.referenceWasmFunctionHeapType(SLOT_READER_WASM_TYPE)
        val writerSig = typeCodegenContext.referenceWasmFunctionHeapType(SLOT_WRITER_WASM_TYPE)
        val allocateSig = typeCodegenContext.referenceWasmFunctionHeapType(SLOT_ALLOCATE_WASM_TYPE)

        // every slot gets a descriptor, funcref-typed ones included (the
        // value crosses the erased channel inside the stdlib's box)
        val layoutSlots = plan.slots.withIndex()
        for ([slotIndex, field] in layoutSlots) {
            val structIndex = payloadFieldIndexOf(slotIndex + 1)
            val structFieldType = plan.currentStruct.fields[structIndex].type

            // ---- reader: (payload anyref) -> anyref — cast to this
            // layout's payload, struct.get, box primitives
            val pLocal = WasmLocal(0, "p", WasmAnyRef, true)
            val reader = WasmFunction.Defined(
                name = "__hot_slotRead$" + plan.classKey + "#" + field.name,
                type = readerSig,
                locals = mutableListOf(pLocal),
            )
            with(WasmExpressionBuilder(reader.instructions)) {
                boxSlotValue(field, backendContext, typeCodegenContext, declarationCodegenContext, transformer, location) {
                    buildGetLocal(pLocal, location)
                    buildRefCastStatic(payloadHeap, location)
                    buildInstr(fieldReadOpcode(field, backendContext.irBuiltIns), location, payloadGcType, WasmImmediate.StructFieldIdx.get(structIndex))
                }
            }
            declarationCodegenContext.defineSyntheticFunction(slotReaderSignature(plan.classKey, field), reader)

            // ---- writer: (payload anyref, value anyref) — cast the value
            // to the slot's static representation (primitives arrive as
            // their value-class box; cast + read the backing field — the
            // unboxIntrinsic pattern), struct.set
            val npLocal = WasmLocal(0, "np", WasmAnyRef, true)
            val vLocal = WasmLocal(1, "v", WasmAnyRef, true)
            val writer = WasmFunction.Defined(
                name = "__hot_slotWrite$" + plan.classKey + "#" + field.name,
                type = writerSig,
                locals = mutableListOf(npLocal, vLocal),
            )
            with(WasmExpressionBuilder(writer.instructions)) {
                buildGetLocal(npLocal, location)
                buildRefCastStatic(payloadHeap, location)
                buildGetLocal(vLocal, location)
                unboxSlotValue(field, backendContext, typeCodegenContext, declarationCodegenContext, transformer, location)
                buildStructSet(payloadGcType, structIndex, location)
            }
            declarationCodegenContext.defineSyntheticFunction(slotWriterSignature(plan.classKey, field), writer)
        }

        // ---- allocate: () -> anyref — a FRESH payload of this layout with
        // type-default slots (the update-instance target's starting state)
        val allocate = WasmFunction.Defined(
            name = "__hot_slotAllocate$" + plan.classKey,
            type = allocateSig,
            locals = mutableListOf(),
        )
        with(WasmExpressionBuilder(allocate.instructions)) {
            buildConstI32(0, location) // Any::_hashCode (inherited; migration copies the live one)
            // the $layout: the module's layout global (set by this
            // load) — migrated payloads come out current
            buildGetGlobal(FieldGlobalSymbol(layoutGlobalKey(plan.classId)), location)
            // the STRUCT tail — the struct is the arity authority
            for (structField in plan.currentStruct.fields.drop(2)) {
                generateDefaultInitializerForType(structField.type, this)
            }
            buildStructNew(payloadGcType, location)
        }
        declarationCodegenContext.defineSyntheticFunction(slotAllocateSignature(plan.classKey), allocate)

        // ---- record the class registration: the link's init emits the
        // register call (dispatch halves + allocate closure), the
        // per-slot addSlot calls and the layout-global store from it
        linkerDataContext.addHotLayoutBinding(
            plan.irClass,
            HotLayoutBinding(
                classId = plan.classId,
                hasItable = plan.irClass.owner.hasInterfaceSuperClass(),
                tags = layoutSlots.flatMap { iv ->
                    val structFieldType = plan.currentStruct.fields[payloadFieldIndexOf(iv.index + 1)].type
                    listOf(
                        slotNameTag(iv.value),
                        slotTypeTag(iv.value, structFieldType, typeCodegenContext::classReferenceKey, backendContext.irBuiltIns),
                    )
                },
                readerKeys = layoutSlots.map { iv -> slotReaderSignature(plan.classKey, iv.value) },
                writerKeys = layoutSlots.map { iv -> slotWriterSignature(plan.classKey, iv.value) },
                allocateKey = slotAllocateSignature(plan.classKey),
            )
        )
        // the init passes the closures as ref.funcs — declare every one
        for (iv in layoutSlots) {
            linkerDataContext.addUsedAsWasmRawFunctionReference(slotReaderSignature(plan.classKey, iv.value))
            linkerDataContext.addUsedAsWasmRawFunctionReference(slotWriterSignature(plan.classKey, iv.value))
        }
        linkerDataContext.addUsedAsWasmRawFunctionReference(slotAllocateSignature(plan.classKey))
    }

    // ------------------------------------------------- slot value erasure

    internal fun fieldReadOpcode(field: IrField, irBuiltIns: IrBuiltIns): WasmOp = when (field.type) {
        irBuiltIns.charType -> WasmOp.STRUCT_GET_U
        irBuiltIns.booleanType, irBuiltIns.byteType, irBuiltIns.shortType -> WasmOp.STRUCT_GET_S
        else -> WasmOp.STRUCT_GET
    }

    /** Box a raw slot value for the erased channel — [pushValue] emits
     *  the value (the stack discipline differs per branch, so it is
     *  pushed THROUGH this helper): funcrefs ride inside the stdlib's
     *  HotFnBox (they are not anyref subtypes — the func and any
     *  hierarchies are disjoint); every INLINE-LIKE type (primitives
     *  and user value classes alike) boxes through the ordinary CELL
     *  path, the raw-wasm form of `generateBox` (boxing is cold —
     *  migration and old-code access); refs pass through; externref
     *  internalizes. [unboxSlotValue] is the mirror. */
    internal fun WasmExpressionBuilder.boxSlotValue(
        field: IrField,
        backendContext: WasmBackendContext,
        typeCodegenContext: WasmTypeCodegenContext,
        declarationCodegenContext: WasmDeclarationCodegenContext,
        transformer: WasmModuleTypeTransformer,
        location: SourceLocation,
        pushValue: WasmExpressionBuilder.() -> Unit,
    ) {
        val wasmType = transformer.transformFieldType(field.type)
        if (isFuncrefSlot(wasmType)) {
            // the value rides inside the stdlib's HotFnBox, constructed
            // raw — the same cell-path shape as value-class boxing
            val boxClass = backendContext.wasmSymbols.hotFnBoxClass
            buildGetGlobal(declarationCodegenContext.referenceGlobalVTable(boxClass), location)
            buildRefNull(WasmHeapType.Simple.None, location) // HotFnBox has no interface supertypes
            buildGetGlobal(declarationCodegenContext.referenceRttiGlobal(boxClass), location)
            buildConstI32(0, location) // Any::_hashCode
            // the payload's $layout: HotFnBox is stdlib-declared (a
            // foreign class from any boxing module) — the image's
            // current-layout map, as in every cold-path stamp
            typeCodegenContext.referenceHeapType(backendContext.wasmSymbols.hotLayoutClass)
            val boxPlan = payloadLayoutPlan(boxClass.owner, typeCodegenContext::classReferenceKey, backendContext, transformer)
            buildConstI64(boxPlan.classId, location)
            buildCall(declarationCodegenContext.referenceFunction(backendContext.wasmSymbols.hotLayoutsCurrentOf), location)
            pushValue()
            buildStructNew(typeCodegenContext.referencePayloadLayoutGcType(boxClass), location)
            buildStructNew(typeCodegenContext.referenceGcType(boxClass), location)
            return
        }
        val inlinedClass = backendContext.inlineClassesUtils.getInlinedClass(field.type)
        if (inlinedClass != null) {
            // generateBox, raw: cell parts BELOW the value, then the two
            // struct.news close over everything in the right order
            val symbol = inlinedClass.symbol
            buildGetGlobal(declarationCodegenContext.referenceGlobalVTable(symbol), location)
            if (inlinedClass.hasInterfaceSuperClass()) {
                buildGetGlobal(declarationCodegenContext.referenceGlobalClassITable(symbol), location)
            } else {
                buildRefNull(WasmHeapType.Simple.None, location)
            }
            buildGetGlobal(declarationCodegenContext.referenceRttiGlobal(symbol), location)
            buildConstI32(0, location) // Any::_hashCode
            // the payload's $layout: the class's CURRENT layout, from
            // the image — boxing is a COLD path (migration, old-code
            // access), so no layout global to read
            typeCodegenContext.referenceHeapType(backendContext.wasmSymbols.hotLayoutClass)
            val plan = payloadLayoutPlan(inlinedClass, typeCodegenContext::classReferenceKey, backendContext, transformer)
            buildConstI64(plan.classId, location)
            buildCall(declarationCodegenContext.referenceFunction(backendContext.wasmSymbols.hotLayoutsCurrentOf), location)
            pushValue()
            buildStructNew(typeCodegenContext.referencePayloadLayoutGcType(symbol), location)
            buildStructNew(typeCodegenContext.referenceGcType(symbol), location)
            return
        }
        pushValue()
        if (wasmType is WasmExternRef || wasmType is WasmRefNullExternrefType) {
            buildInstr(WasmOp.EXTERN_INTERNALIZE, location)
        }
    }

    // ------------------------------------------------------- object adoption

    /** Host module of the synthesized object-adoption imports. */
    const val HOT_ADOPT_MODULE = "host"

    /**
     * Prefix of the synthesized host import that gives `object` (and
     * companion) declarations defvar semantics across redefinitions (see
     * WasmHotReloadObjectAdoptionLowering). The import name is
     * `<object name>#<referenceKey.hashCode() in hex>`: the hashCode is
     * the one invariant that survives BOTH the klib round trip and edits
     * of the declaring file, so the host finds the previous live
     * instance's `__hot_entry$…#<suffix>` export by it — or supplies null
     * when none exists (cold boot, a brand-new object).
     */
    const val HOT_ADOPT_PREFIX = "__hot_adopt$"

    /** The import name of [declaration] when it is a synthesized adopt
     *  function, else null. */
    fun hotAdoptImportName(declaration: IrSimpleFunction): String? =
        declaration.name.asString().takeIf { it.startsWith(HOT_ADOPT_PREFIX) }

    fun hotAdoptFunctionName(objectName: String, referenceKey: IdSignature): String =
        HOT_ADOPT_PREFIX + objectName + "#" + Integer.toHexString(referenceKey.hashCode())

    // --------------------------------------------- the fdefn binding encoding
    // (DESIGN §15): the exact binding name — watName#<sig-suffix> (the
    // suffix stays: IdSignature.toString() is not injective)

    fun bindingName(watName: String, uniqueSuffix: String): String = "$watName#$uniqueSuffix"

    /** The fdefn cell's wasm type — STANDALONE: a rec group is the
     *  canonicalization unit, and grouped members never unify with
     *  standalones declared elsewhere. */
    val FDEFN_TYPE: IdSignature = synthetic("fdefn")
    const val FDEFN_FN_FIELD = 0

    fun synthetic(name: String): IdSignature =
        IdSignature.CommonSignature("__HOT__", name, null, 0, null)

    fun cellGlobalKey(bindingName: String): IdSignature = synthetic("cell<$bindingName>")

    /** The per-class MODULE-LOCAL layout-object global's key: the init
     *  stores the registered layout into it and every allocation of the
     *  class reads it — one instruction. */
    fun layoutGlobalKey(classId: Long): IdSignature = synthetic("layout<$classId>")

    fun hotReloadImplKey(watName: String, realKey: IdSignature): IdSignature {
        val uniqueSuffix = Integer.toHexString(realKey.hashCode())
        return IdSignature.CommonSignature("hot.reload", "impl<${bindingName(watName, uniqueSuffix)}>", null, 0, null)
    }

}

/** The linker-data record for one class registration into the image's
 *  CLASS REGISTRY: the link's init emits the register call (the
 *  class's vtable and itable globals + the allocate closure), the
 *  per-slot addSlot calls and the layout-global store from it. */
class HotLayoutBinding(
    /** the class's reference key — filled by
     *  [WasmLinkerDataCodegenContext.addHotLayoutBinding] */
    var classKey: IdSignature? = null,
    val classId: Long,
    /** whether the class has an itable global (interface supertypes) */
    val hasItable: Boolean,
    /** interleaved (name tag, type tag) per slot */
    val tags: List<Long>,
    val readerKeys: List<IdSignature>,
    val writerKeys: List<IdSignature>,
    val allocateKey: IdSignature,
)

/** Unbox an anyref slot value (the erased channel's currency) to the
 *  field's wasm representation: a funcref arrives inside the stdlib's
 *  box (it cannot cross as itself); a primitive arrives as its
 *  value-class box (cast + backing-field read through the box's
 *  payload — the unboxIntrinsic pattern); a declared ref casts to its
 *  heap; externref needs the externalize conversion (externref is not
 *  an anyref subtype). The READERS' mirror lives in the layout
 *  functions; this half is the field-access codegen's slow-path tail. */
internal fun WasmExpressionBuilder.unboxSlotValue(
    field: IrField,
    backendContext: WasmBackendContext,
    typeCodegenContext: WasmTypeCodegenContext,
    declarationCodegenContext: WasmDeclarationCodegenContext,
    transformer: WasmModuleTypeTransformer,
    location: SourceLocation,
) {
    val wasmType = transformer.transformFieldType(field.type)
    if (HotReloadPayloads.isFuncrefSlot(wasmType)) {
        // the value rides inside the stdlib's HotFnBox — the unbox
        // mirror of value-class boxing: cast to the box, payload,
        // field; then the field's own heap (typed signature or the
        // abstract func) comes back via the same cast every other
        // unbox arm uses
        val boxClass = backendContext.wasmSymbols.hotFnBoxClass
        val boxPlan = HotReloadPayloads.payloadLayoutPlan(
            boxClass.owner, typeCodegenContext::classReferenceKey, backendContext, transformer,
        )
        val fnIndex = boxPlan.currentStruct.fields.indexOfFirst { it.name == "fn" }
        buildRefCastStatic(typeCodegenContext.referenceHeapType(boxClass), location)
        buildStructGet(typeCodegenContext.referenceGcType(boxClass), HotReloadPayloads.CLASS_PAYLOAD_FIELD_ID, location)
        buildRefCastStatic(typeCodegenContext.referencePayloadLayoutHeapType(boxClass), location)
        buildInstr(
            WasmOp.STRUCT_GET, location,
            typeCodegenContext.referencePayloadLayoutGcType(boxClass), WasmImmediate.StructFieldIdx.get(fnIndex),
        )
        when (wasmType) {
            is WasmRefType -> buildRefCastStatic(wasmType.heapType, location)
            is WasmRefNullType -> buildRefCastNullStatic(wasmType.heapType, location)
            else -> {}
        }
        return
    }
    val inlinedClass = backendContext.inlineClassesUtils.getInlinedClass(field.type)
    when {
        inlinedClass != null -> {
            val backing = getInlineClassBackingField(inlinedClass)
            val boxPlan = HotReloadPayloads.payloadLayoutPlan(
                inlinedClass, typeCodegenContext::classReferenceKey, backendContext, transformer,
            )
            val backingIndex = boxPlan.currentStruct.fields.indexOfFirst { it.name == backing.name.toString() }
            buildRefCastStatic(typeCodegenContext.referenceHeapType(inlinedClass.symbol), location)
            buildStructGet(typeCodegenContext.referenceGcType(inlinedClass.symbol), HotReloadPayloads.CLASS_PAYLOAD_FIELD_ID, location) // the cell's payload
            buildRefCastStatic(typeCodegenContext.referencePayloadLayoutHeapType(inlinedClass.symbol), location)
            buildInstr(
                HotReloadPayloads.fieldReadOpcode(backing, backendContext.irBuiltIns), location,
                typeCodegenContext.referencePayloadLayoutGcType(inlinedClass.symbol), WasmImmediate.StructFieldIdx.get(backingIndex),
            )
        }
        wasmType is WasmExternRef || wasmType is WasmRefNullExternrefType ->
            buildInstr(WasmOp.EXTERN_EXTERNALIZE, location)
        else -> {
            // a declared ref: cast to the field's heap — the NULL variant
            // for nullable fields (a null must not trap the erased
            // channel). Interface-typed refs have NO wasm type: the
            // transformer erases them to the Any heap, and so does this.
            val klass = field.type.erasedUpperBound.takeIf { !it.isInterface }
                ?: backendContext.irBuiltIns.anyClass.owner
            val heap = typeCodegenContext.referenceHeapType(klass.symbol)
            if (wasmType is WasmRefNullType) {
                buildRefCastNullStatic(heap, location)
            } else {
                buildRefCastStatic(heap, location)
            }
        }
    }
}
