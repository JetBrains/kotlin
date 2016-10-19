package org.jetbrains.kotlin.backend.native.llvm

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

internal val FunctionDescriptor.symbolName: String
    get() = "kfun:" + this.fqNameSafe.toString() // FIXME: add signature

internal val ClassDescriptor.typeInfoSymbolName: String
    get() = "ktype:" + this.fqNameSafe.toString()