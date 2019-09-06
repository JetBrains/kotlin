/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objc

import llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.descriptors.konan.CurrentKlibModuleOrigin

/**
 * This class provides methods to generate Objective-C RTTI and other data.
 * It is mostly based on `clang/lib/CodeGen/CGObjCMac.cpp`, and supports only subset of operations
 * required for our purposes (thus simplified).
 *
 * [finishModule] must be called exactly once after all required data was generated.
 */
internal class ObjCDataGenerator(val codegen: CodeGenerator) {

    val context = codegen.context

    fun finishModule() {
        addModuleClassList(
                definedClasses,
                "OBJC_LABEL_CLASS_$",
                "__DATA,__objc_classlist,regular,no_dead_strip"
        )
    }

    private val selectorRefs = mutableMapOf<String, ConstPointer>()
    private val classRefs = mutableMapOf<String, ConstPointer>()

    fun genSelectorRef(selector: String): ConstPointer = selectorRefs.getOrPut(selector) {
        val literal = selectors.get(selector)
        val global = codegen.staticData.placeGlobal("OBJC_SELECTOR_REFERENCES_", literal)
        global.setLinkage(LLVMLinkage.LLVMPrivateLinkage)
        LLVMSetExternallyInitialized(global.llvmGlobal, 1)
        global.setAlignment(codegen.runtime.pointerAlignment)
        global.setSection("__DATA,__objc_selrefs,literal_pointers,no_dead_strip")

        context.llvm.compilerUsedGlobals += global.llvmGlobal

        global.pointer
    }

    fun genClassRef(name: String): ConstPointer = classRefs.getOrPut(name) {
        val classGlobal = getClassGlobal(name, isMetaclass = false)
        val global = codegen.staticData.placeGlobal("OBJC_CLASSLIST_REFERENCES_\$_", classGlobal).also {
            it.setLinkage(LLVMLinkage.LLVMPrivateLinkage)
            it.setSection("__DATA,__objc_classrefs,regular,no_dead_strip")
            it.setAlignment(codegen.runtime.pointerAlignment)
        }

        context.llvm.compilerUsedGlobals += global.pointer.llvm

        global.pointer.bitcast(pointerType(int8TypePtr))
    }

    val classObjectType = codegen.runtime.getStructType("_class_t")

    fun exportClass(name: String) {
        context.llvm.usedGlobals += getClassGlobal(name, isMetaclass = false).llvm
        context.llvm.usedGlobals += getClassGlobal(name, isMetaclass = true).llvm
    }

    private fun getClassGlobal(name: String, isMetaclass: Boolean): ConstPointer {
        val prefix = if (isMetaclass) {
            "OBJC_METACLASS_\$_"
        } else {
            "OBJC_CLASS_\$_"
        }

        val globalName = prefix + name

        // TODO: refactor usages and use [Global] class.
        val llvmGlobal = LLVMGetNamedGlobal(context.llvmModule, globalName) ?:
                codegen.importGlobal(globalName, classObjectType, CurrentKlibModuleOrigin)

        return constPointer(llvmGlobal)
    }

    private val emptyCache = constPointer(
            codegen.importGlobal(
                    "_objc_empty_cache",
                    codegen.runtime.getStructType("_objc_cache"),
                    CurrentKlibModuleOrigin
            )
    )

    fun emitEmptyClass(name: String, superName: String) {
        emitClass(name, superName, instanceMethods = emptyList())
    }

    class Method(val selector: String, val encoding: String, val imp: ConstPointer)

    fun emitClass(name: String, superName: String, instanceMethods: List<Method>) {
        val runtime = context.llvm.runtime
        fun struct(name: String) = runtime.getStructType(name)

        val classRoType = struct("_class_ro_t")
        val methodType = struct("_objc_method")
        val methodListType = struct("__method_list_t")
        val protocolListType = struct("_objc_protocol_list")
        val ivarListType = struct("_ivar_list_t")
        val propListType = struct("_prop_list_t")

        val classNameLiteral = classNames.get(name)

        fun emitInstanceMethodList(): ConstPointer {
            if (instanceMethods.isEmpty()) return NullPointer(methodListType)

            val methodStructs = instanceMethods.map {
                Struct(methodType, selectors.get(it.selector), encodings.get(it.encoding), it.imp.bitcast(int8TypePtr))
            }

            val methodList = Struct(
                    Int32(LLVMABISizeOfType(codegen.llvmTargetData, methodType).toInt()),
                    Int32(instanceMethods.size),
                    ConstArray(methodType, methodStructs)
            )

            val globalName = "\u0001l_OBJC_\$_INSTANCE_METHODS_$name"
            val global = context.llvm.staticData.placeGlobal(globalName, methodList).also {
                it.setLinkage(LLVMLinkage.LLVMPrivateLinkage)
                it.setAlignment(runtime.pointerAlignment)
                it.setSection("__DATA, __objc_const")
            }

            context.llvm.compilerUsedGlobals += global.llvmGlobal

            return global.pointer.bitcast(pointerType(methodListType))
        }

        fun buildClassRo(isMetaclass: Boolean): ConstPointer {
            // TODO: add NonFragileABI_Class_CompiledByARC flag?

            val flags: Int
            val start: Int
            val size: Int
            // TODO: stop using hard-coded values.
            if (isMetaclass) {
                flags = 1
                start = 40
                size = 40
            } else {
                flags = 0
                start = 8
                size = 8
            }

            val fields = mutableListOf<ConstValue>()

            fields += Int32(flags)
            fields += Int32(start)
            fields += Int32(size)
            fields += NullPointer(int8Type) // ivar layout name
            fields += classNameLiteral
            fields += if (isMetaclass) NullPointer(methodListType) else emitInstanceMethodList()
            fields += NullPointer(protocolListType)
            fields += NullPointer(ivarListType)
            fields += NullPointer(int8Type) // ivar layout
            fields += NullPointer(propListType)

            val roValue = Struct(classRoType, fields)

            val roLabel = if (isMetaclass) {
                "\u0001l_OBJC_METACLASS_RO_\$_"
            } else {
                "\u0001l_OBJC_CLASS_RO_\$_"
            } + name

            val roGlobal = context.llvm.staticData.placeGlobal(roLabel, roValue).also {
                it.setLinkage(LLVMLinkage.LLVMPrivateLinkage)
                it.setAlignment(runtime.pointerAlignment)
                it.setSection("__DATA, __objc_const")
            }

            return roGlobal.pointer
        }

        fun buildClassObject(
                isMetaclass: Boolean,
                isa: ConstPointer,
                superClass: ConstPointer,
                classRo: ConstPointer
        ): ConstPointer {
            val fields = mutableListOf<ConstValue>()

            fields += isa
            fields += superClass
            fields += emptyCache
            val vtableEntryType = pointerType(functionType(int8TypePtr, false, int8TypePtr, int8TypePtr))
            fields += NullPointer(vtableEntryType) // empty vtable
            fields += classRo

            val classObjectValue = Struct(classObjectType, fields)
            val classGlobal = getClassGlobal(name, isMetaclass = isMetaclass)

            LLVMSetInitializer(classGlobal.llvm, classObjectValue.llvm)
            LLVMSetSection(classGlobal.llvm, "__DATA, __objc_data")
            LLVMSetAlignment(classGlobal.llvm, LLVMABIAlignmentOfType(runtime.targetData, classObjectType))

            context.llvm.usedGlobals.add(classGlobal.llvm)

            return classGlobal
        }

        val metaclassObject = buildClassObject(
                isMetaclass = true,
                isa = getClassGlobal("NSObject", isMetaclass = true),
                superClass = getClassGlobal(superName, isMetaclass = true),
                classRo = buildClassRo(isMetaclass = true)
        )

        val classObject = buildClassObject(
                isMetaclass = false,
                isa = metaclassObject,
                superClass = getClassGlobal(superName, isMetaclass = false),
                classRo = buildClassRo(isMetaclass = false)
        )

        definedClasses.add(classObject)
    }

    private val definedClasses = mutableListOf<ConstPointer>()

    private fun addModuleClassList(elements: List<ConstPointer>, name: String, section: String) {
        if (elements.isEmpty()) return

        val global = context.llvm.staticData.placeGlobalArray(
                name,
                int8TypePtr,
                elements.map { it.bitcast(int8TypePtr) }
        )

        global.setAlignment(
                LLVMABIAlignmentOfType(
                        context.llvm.runtime.targetData,
                        LLVMGetInitializer(global.llvmGlobal)!!.type
                )
        )

        global.setSection(section)

        context.llvm.compilerUsedGlobals += global.llvmGlobal
    }

    private val classNames =
            CStringLiteralsTable("OBJC_CLASS_NAME_", "__TEXT,__objc_classname,cstring_literals")

    private val selectors =
            CStringLiteralsTable("OBJC_METH_VAR_NAME_",  "__TEXT,__objc_methname,cstring_literals")

    private val encodings =
            CStringLiteralsTable("OBJC_METH_VAR_TYPE_", "__TEXT,__objc_methtype,cstring_literals")

    private inner class CStringLiteralsTable(val label: String, val section: String) {

        private val literals = mutableMapOf<String, ConstPointer>()

        fun get(value: String) = literals.getOrPut(value) {
            val bytes = value.toByteArray(Charsets.UTF_8).map { Int8(it) } + Int8(0)
            val global = context.llvm.staticData.placeGlobalArray(label, int8Type, bytes)

            global.setConstant(true)
            global.setLinkage(LLVMLinkage.LLVMPrivateLinkage)
            global.setSection(section)
            LLVMSetUnnamedAddr(global.llvmGlobal, 1)
            global.setAlignment(1)

            context.llvm.compilerUsedGlobals += global.llvmGlobal

            global.pointer.getElementPtr(0)
        }
    }
}
