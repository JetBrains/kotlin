/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.common.ir.isOverridableOrOverrides
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.backend.wasm.ast.*
import org.jetbrains.kotlin.backend.wasm.utils.hasExcludedFromCodegenAnnotation
import org.jetbrains.kotlin.backend.wasm.utils.hasSkipRTTIAnnotation
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.jsAssignment
import org.jetbrains.kotlin.ir.backend.js.utils.functionSignature
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.backend.js.utils.sanitizeName
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.js.backend.ast.JsArrayLiteral
import org.jetbrains.kotlin.js.backend.ast.JsBlock
import org.jetbrains.kotlin.js.backend.ast.JsNameRef
import org.jetbrains.kotlin.js.backend.ast.JsStringLiteral
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addIfNotNull

private val IrClass.superClasses: List<IrClass>
    get() = superTypes.map { it.classifierOrFail.owner as IrClass }

private fun IrDeclaration.collectFunctions(): List<IrFunction> =
    when {
        hasExcludedFromCodegenAnnotation() -> emptyList()
        this is IrFunction -> listOf(this)
        this is IrClass -> this.declarations.flatMap { it.collectFunctions() }
        else -> emptyList()
    }


private fun <T> List<T>.elementToIdMap(): Map<T, Int> =
    mapIndexed { idx, el -> el to idx }.toMap()


class IrModuleToWasm(private val backendContext: WasmBackendContext) {

    private val anyClass = backendContext.irBuiltIns.anyClass.owner

    fun generateModule(module: IrModuleFragment): WasmCompilerResult {

        // Collect IR nodes

        val irPackageFragments: List<IrPackageFragment> =
            module.files + listOf(backendContext.internalPackageFragment)

        val irDeclarations: List<IrDeclaration> =
            irPackageFragments
                .flatMap { it.declarations }
                .filter { !it.hasExcludedFromCodegenAnnotation() }

        val irFunctions: List<IrFunction> =
            irDeclarations.flatMap { it.collectFunctions() }

        val irTopLevelFields: List<IrField> =
            irDeclarations.filterIsInstance<IrField>()

        val irBroadClasses: List<IrClass> =
            irDeclarations.filterIsInstance<IrClass>()

        // Generate Wasm types

        val functionIds: Map<IrFunction, Int> =
            irFunctions.elementToIdMap()

        // TODO: Merge equivalent function types

        val functionTypes =
            irFunctions.map { it }

        val nameTable = generateWatTopLevelNames(irPackageFragments)
        val typeInfo = collectTypeInfo(irDeclarations)

        val wasmTypeNames = generateWatTypeNames(irPackageFragments)
        val context = WasmCodegenContext(nameTable, wasmTypeNames, backendContext, typeInfo)

        val virtFuns = mutableListOf<String>()
        for ((f, id) in typeInfo.virtualFunctionIds) {
            virtFuns.add(id, context.getGlobalName(f))
        }

        val funcRefTable = WasmFuncrefTable(virtFuns)

        val wasmDeclarations = irDeclarations.mapNotNull { it.accept(DeclarationTransformer(), context) }
        val exports = generateExports(module, context)
        val namedTypes = generateNamedTypes(module, context)
        val wasmStart = WasmStart(context.getGlobalName(backendContext.startFunction))

        val gcOptIn = WasmCustomSection("  (gc_feature_opt_in 3)")

        val typeInfoSizeInPages = (typeInfo.typeInfoSizeInBytes / 65_536) + 1
        val memory = WasmMemory(typeInfoSizeInPages, typeInfoSizeInPages)
        val wasmModule = WasmModule(
            listOf(gcOptIn, memory) +
                    context.imports +
                    namedTypes +
                    funcRefTable +
                    wasmDeclarations +
                    typeInfo.datas +
                    listOf(wasmStart) +
                    exports
        )


        val wat = wasmModuleToWat(wasmModule)
        return WasmCompilerResult(wat, generateStringLiteralsSupport(context.stringLiterals))
    }

    private fun collectTypeInfo(irDeclarations: List<IrDeclaration>): WasmTypeInfo {
        val typeInfo = WasmTypeInfo()
        val classes = irDeclarations
            .filterIsInstance<IrClass>()
            .filter { !it.isAnnotationClass && !it.hasSkipRTTIAnnotation() && !it.hasExcludedFromCodegenAnnotation() }

        val classesSorted = DFS.topologicalOrder(classes) { it.superClasses }.reversed()

        var classId = 0
        var ifaceId = 0

        for (irClass in classesSorted) {
            if (irClass.isInterface) {
                typeInfo.interfaces[irClass] = InterfaceMetadata(ifaceId++)
                continue
            }

            val superClasses = irClass.superClasses
            val superClassInfo: ClassMetadata = if (irClass.defaultType.isAny()) {
                ClassMetadata(irClass, 0, null, emptyList(), emptyList(), emptyList())
            } else {
                val superClass: IrClass = superClasses.singleOrNull { !it.isInterface } ?: anyClass
                typeInfo.classes[superClass]!!
            }

            fun IrClass.allSuperClasses(): List<IrClass> =
                (this.superClasses + this.superClasses.flatMap { it.allSuperClasses() }).distinct()

            val implementedInterfaces = irClass.allSuperClasses().filter { it.isInterface }

            val virtualFunctions = irClass.declarations
                .filterIsInstance<IrSimpleFunction>()
                .map { it.realOverrideTarget }
                .filter { it.isOverridableOrOverrides }

            for (vf in virtualFunctions) {
                val nextId = typeInfo.virtualFunctionIds.size
                typeInfo.virtualFunctionIds.getOrPut(vf) {
                    nextId
                }
            }

            val signatureToVirtualFunction = virtualFunctions.associateBy { functionSignature(it) }

            val inheritedVirtualMethods: List<VirtualMethodMetadata> = superClassInfo.virtualMethods.map { vm ->
                VirtualMethodMetadata(signatureToVirtualFunction.getValue(vm.signature), vm.signature)
            }

            val newVirtualMethods: List<VirtualMethodMetadata> = signatureToVirtualFunction
                .filterKeys { it !in superClassInfo.virtualMethodsSignatures }
                .map {
                    VirtualMethodMetadata(it.value, it.key)
                }

            val allVirtualMethods = inheritedVirtualMethods + newVirtualMethods

            val classMetadata = ClassMetadata(
                irClass,
                classId,
                superClass = superClassInfo,
                fields = superClassInfo.fields + irClass.declarations.filterIsInstance<IrField>().map { it.symbol },
                interfaces = implementedInterfaces.map { typeInfo.interfaces[it]!! },
                virtualMethods = allVirtualMethods
            )

            typeInfo.classes[irClass] = classMetadata

            val classLmStruct = binaryDataStruct(classMetadata, typeInfo)

            typeInfo.datas.add(WasmData(classId, classLmStruct.toBytes()))
            classId += classLmStruct.sizeInBytes
        }

        typeInfo.typeInfoSizeInBytes = classId
        return typeInfo
    }

    private fun binaryDataStruct(
        classMetadata: ClassMetadata,
        typeInfo: WasmTypeInfo
    ): BinaryDataStruct {
        val lmSuperType = BinaryDataIntField("Super class", classMetadata.superClass?.id ?: -1)

        val lmImplementedInterfacesData = BinaryDataIntArray(
            "data",
            classMetadata.interfaces.map { it.id }
        )
        val lmImplementedInterfacesSize = BinaryDataIntField(
            "size",
            lmImplementedInterfacesData.value.size
        )

        val lmImplementedInterfaces = BinaryDataStruct(
            "Implemented interfaces array", listOf(lmImplementedInterfacesSize, lmImplementedInterfacesData)
        )

        val lmVirtualFunctionsSize = BinaryDataIntField(
            "V-table length",
            classMetadata.virtualMethods.size
        )

        val lmVtable = BinaryDataIntArray(
            "V-table",
            classMetadata.virtualMethods.map { typeInfo.virtualFunctionIds[it.function]!! }
        )

        val lmSignatures = BinaryDataIntArray(
            "Signatures Stub",
            List(classMetadata.virtualMethods.size) { -1 }
        )

        val classLmElements = listOf(lmSuperType, lmVirtualFunctionsSize, lmVtable, lmSignatures, lmImplementedInterfaces)
        val classLmStruct = BinaryDataStruct("Class TypeInfo: ${classMetadata.id} ${classMetadata.ir.fqNameWhenAvailable} ", classLmElements)
        return classLmStruct
    }

    private fun generateStringLiteralsSupport(literals: List<String>): String {
        return JsBlock(
            jsAssignment(
                JsNameRef("stringLiterals", "runtime"),
                JsArrayLiteral(literals.map { JsStringLiteral(it) })
            ).makeStmt()
        ).toString()
    }

    private fun generateNamedTypes(module: IrModuleFragment, context: WasmCodegenContext): List<WasmNamedType> {
        val namedTypes = mutableListOf<WasmNamedType>()
        module.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                if (element is IrAnnotationContainer && element.hasExcludedFromCodegenAnnotation()) return
                element.acceptChildrenVoid(this)
            }

            override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                super.visitSimpleFunction(declaration)
                namedTypes.add(context.wasmFunctionType(declaration))
            }

            override fun visitClass(declaration: IrClass) {
                if (declaration.hasSkipRTTIAnnotation()) return
                super.visitClass(declaration)
                if (declaration.kind == ClassKind.CLASS || declaration.kind == ClassKind.OBJECT) {
                    namedTypes.add(context.wasmStructType(declaration))
                }
            }
        })

        return namedTypes
    }


    private fun generateExports(module: IrModuleFragment, context: WasmCodegenContext): List<WasmExport> {
        val exports = mutableListOf<WasmExport>()
        for (file in module.files) {
            for (declaration in file.declarations) {
                exports.addIfNotNull(generateExport(declaration, context))
            }
        }
        return exports
    }

    private fun generateExport(declaration: IrDeclaration, context: WasmCodegenContext): WasmExport? {
        if (declaration !is IrDeclarationWithVisibility ||
            declaration !is IrDeclarationWithName ||
            declaration !is IrSimpleFunction ||
            declaration.visibility != Visibilities.PUBLIC
        ) {
            return null
        }

        if (!declaration.isExported(context))
            return null

        val internalName = context.getGlobalName(declaration)
        val exportedName = sanitizeName(declaration.name.identifier)

        return WasmExport(
            wasmName = internalName,
            exportedName = exportedName,
            kind = WasmExport.Kind.FUNCTION
        )
    }

}

fun IrFunction.isExported(context: WasmCodegenContext): Boolean =
    fqNameWhenAvailable in context.backendContext.additionalExportedDeclarations
