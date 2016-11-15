package org.jetbrains.kotlin.backend.konan.llvm

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind.*
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

private val symbolNameAnnotation = FqName("kotlin.SymbolName")

private val FunctionDescriptor.signature: String
    get() {
        // TODO: KotlinType.toString seems to use unqualified names

        val extensionReceiverPart = this.extensionReceiverParameter?.let { "${it.type}." } ?: ""

        val argsPart = this.valueParameters.map { it.type }.joinToString(";")

        // TODO: add return type
        // (it is not simple because return type can be changed when overriding)
        return "$extensionReceiverPart($argsPart)"
    }

// TODO: rename to indicate that it has signature included
internal val FunctionDescriptor.functionName: String
    get() = with(this.original) { // basic support for generics
        "$name$signature"
    }

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
        val containingDeclarationPart = containingDeclaration.fqNameSafe.let {
            if (it.isRoot) "" else "$it."
        }
        return "kfun:$containingDeclarationPart$functionName"
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

internal val PropertyDescriptor.symbolName:String
get() = "kvar:${this.containingDeclaration.name.asString()}.${this.name.asString()}"