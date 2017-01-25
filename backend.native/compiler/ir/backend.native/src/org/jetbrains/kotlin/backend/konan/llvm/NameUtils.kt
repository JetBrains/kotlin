package org.jetbrains.kotlin.backend.konan.llvm

import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.ClassKind.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.findOriginalTopMostOverriddenDescriptors
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty


private val symbolNameAnnotation = FqName("konan.SymbolName")

private val exportForCppRuntimeAnnotation = FqName("konan.internal.ExportForCppRuntime")

fun typeToHashString(type: KotlinType): String {
    if (TypeUtils.isTypeParameter(type)) return "GENERIC"

    var hashString = TypeUtils.getClassDescriptor(type)!!.fqNameSafe.asString()
    if (!type.arguments.isEmpty()) {
        hashString += "<${type.arguments.map {
            typeToHashString(it.type)
        }.joinToString(",")}>"
    }
    
    if (type.isMarkedNullable) hashString += "?"
    return hashString
}

private val FunctionDescriptor.signature: String
    get() {
        val extensionReceiverPart = this.extensionReceiverParameter?.let { "${it.type}." } ?: ""
        val actualDescriptor = this.findOriginalTopMostOverriddenDescriptors().firstOrNull() ?: this

        val argsPart = actualDescriptor.valueParameters.map {
            typeToHashString(it.type)
        }.joinToString(";")

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
                return getStringValue(it)!!
            } else {
                // ignore; TODO: report compile error
            }
        }

        this.annotations.findAnnotation(exportForCppRuntimeAnnotation)?.let {
            val name = getStringValue(it) ?: this.name.asString()
            return name // no wrapping currently required
        }

        val containingDeclarationPart = containingDeclaration.fqNameSafe.let {
            if (it.isRoot) "" else "$it."
        }
        return "kfun:$containingDeclarationPart$functionName"
    }

private fun getStringValue(annotation: AnnotationDescriptor): String? {
    annotation.allValueArguments.values.ifNotEmpty {
        val stringValue = this.single() as StringValue
        return stringValue.value
    }

    return null
}

internal val ClassDescriptor.symbolName: String
    get() = when (this.kind) {
        CLASS -> "kclass:"
        INTERFACE -> "kinf:"
        OBJECT -> "kclass:"
        ENUM_CLASS -> "kclass:"
        ENUM_ENTRY -> "kclass:"
        else -> TODO("fixme: " + this.kind)
    } + fqNameSafe

internal val ClassDescriptor.typeInfoSymbolName: String
    get() = "ktype:" + this.fqNameSafe.toString()

internal val PropertyDescriptor.symbolName:String
get() = "kvar:${this.containingDeclaration.name.asString()}.${this.name.asString()}"