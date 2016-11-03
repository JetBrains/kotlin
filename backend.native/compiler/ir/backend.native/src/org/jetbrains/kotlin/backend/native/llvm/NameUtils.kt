package org.jetbrains.kotlin.backend.native.llvm

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind.*
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

private val symbolNameAnnotation = FqName("kotlin.SymbolName")

internal val FunctionDescriptor.symbolName: String
    get() {
        this.annotations.findAnnotation(symbolNameAnnotation)?.let {
            if (this.isExternal) {
                val nameValue = it.allValueArguments.values.single() as StringValue
                return nameValue.value
            } else {
                // ignore; TODO: report compile error
            }
        }
        return "kfun:" + this.fqNameSafe.toString() // FIXME: add signature
    }

internal val ClassDescriptor.symbolName: String
    get() = when (this.kind) {
        CLASS -> "kclass:"
        INTERFACE -> "kinf:"
        OBJECT -> "kclass:"
        else -> TODO("fixme: " + this.kind)
    } + fqNameSafe

internal val ClassDescriptor.typeInfoSymbolName: String
    get() = "ktype:" + this.fqNameSafe.toString()