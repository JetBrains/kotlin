package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*


/**
 * Creates const array-typed global with given name and value.
 * Returns pointer to the first element of the array.
 *
 * If [elements] is empty, then null pointer is returned.
 */
internal fun StaticData.placeGlobalConstArray(name: String,
                                              elemType: LLVMTypeRef,
                                              elements: List<ConstValue>): ConstPointer {
    if (elements.isNotEmpty()) {
        val global = this.placeGlobalArray(name, elemType, elements)
        global.setConstant(true)
        return global.pointer.getElementPtr(0)
    } else {
        return NullPointer(elemType)
    }
}

internal fun StaticData.createAlias(name: String, aliasee: ConstPointer): ConstPointer {
    val alias = LLVMAddAlias(context.llvmModule, aliasee.llvmType, aliasee.llvm, name)!!
    return constPointer(alias)
}
