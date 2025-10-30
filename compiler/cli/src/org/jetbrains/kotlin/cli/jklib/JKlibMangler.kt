/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jklib

import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinExportChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorBasedKotlinManglerImpl
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorExportCheckerVisitor
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrBasedKotlinManglerImpl
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrExportCheckerVisitor
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrMangleComputer
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.CompositeAnnotations
import org.jetbrains.kotlin.descriptors.containingPackage
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.load.java.JSPECIFY_NULL_MARKED_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.isJavaField
import org.jetbrains.kotlin.load.java.typeEnhancement.ENHANCED_NULLABILITY_ANNOTATIONS
import org.jetbrains.kotlin.load.java.typeEnhancement.hasEnhancedNullability
import org.jetbrains.kotlin.load.kotlin.JvmType
import org.jetbrains.kotlin.load.kotlin.JvmTypeFactory
import org.jetbrains.kotlin.load.kotlin.TypeMappingConfiguration
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.load.kotlin.mapType
import org.jetbrains.kotlin.load.kotlin.signature
import org.jetbrains.kotlin.load.kotlin.signatures
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations

/*
 * TODO:
 *  Code in this file is mostly copied from `JvmMangler.kt` in the Kotlin compiler. The difference is that these custom manglers aim to
 *  compute plain JVM signatures for Java methods. For example instead of this:
 *  ```
 *  java/lang/Comparator.thenComparing(java.util.function.Function<in|1:0?,out|0:0?>?){0§<kotlin.Comparable<in|0:0?>?>}
 *  ```
 *  We will get:
 *  ```
 *  java/util/Comparator.thenComparing(Ljava/util/function/Function;)Ljava/util/Comparator;
 *  ```
 *  This logic aims to fix signature differences between K1 and K2. And thus, when the K1 klib deserialization part of the K2CL pipeline
 *  transitions to K2, code in this file should no longer be needed.
 */

object JKlibIrMangler : IrBasedKotlinManglerImpl() {
    private class JKlibIrExportChecker(compatibleMode: Boolean) : IrExportCheckerVisitor(compatibleMode) {
        override fun IrDeclaration.isPlatformSpecificExported() = false
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun IrDeclaration.signatureString(compatibleMode: Boolean): String {
        if (!getPackageFragment().packageFqName.asString().isKotlinPackage() && isJavaBackedCallable()) {
            (descriptor as? CallableDescriptor)?.computeJvmSignatureSafe()?.let {
                return it
            }
        }
        // Copied from `super`
        return getMangleComputer(MangleMode.SIGNATURE, compatibleMode).computeMangle(this)
    }

    private class JKlibIrManglerComputer(builder: StringBuilder, mode: MangleMode, compatibleMode: Boolean) : IrMangleComputer(builder, mode, compatibleMode) {
        override fun copy(newMode: MangleMode): IrMangleComputer =
            JKlibIrManglerComputer(builder, newMode, compatibleMode)

        override fun addReturnTypeSpecialCase(function: IrFunction): Boolean = false

        @OptIn(ObsoleteDescriptorBasedAPI::class)
        override fun mangleTypePlatformSpecific(type: IrType, tBuilder: StringBuilder) {
            if (type.hasAnnotation(JvmAnnotationNames.ENHANCED_NULLABILITY_ANNOTATION)) {
                tBuilder.append(MangleConstant.ENHANCED_NULLABILITY_MARK)
            }
        }
    }

    override fun getExportChecker(compatibleMode: Boolean): KotlinExportChecker<IrDeclaration> = JKlibIrExportChecker(compatibleMode)

    override fun getMangleComputer(mode: MangleMode, compatibleMode: Boolean): KotlinMangleComputer<IrDeclaration> =
        JKlibIrManglerComputer(StringBuilder(256), mode, compatibleMode)
}

class JKlibDescriptorMangler(private val mainDetector: MainFunctionDetector?) : DescriptorBasedKotlinManglerImpl() {
    private object ExportChecker : DescriptorExportCheckerVisitor() {
        override fun DeclarationDescriptor.isPlatformSpecificExported() = true
    }

    override fun DeclarationDescriptor.signatureString(compatibleMode: Boolean): String {
        if (this.containingPackage()?.asString()?.isKotlinPackage() == false && this is JavaCallableMemberDescriptor || containingDeclaration is JavaClassDescriptor) {
            (this as? CallableDescriptor)?.computeJvmSignatureSafe()?.let {
                return it
            }
        }
        // Copied from `super`
        return getMangleComputer(MangleMode.SIGNATURE, compatibleMode).computeMangle(this)
    }

    private class JKlibDescriptorManglerComputer(
        builder: StringBuilder,
        private val mainDetector: MainFunctionDetector?,
        mode: MangleMode
    ) : DescriptorMangleComputer(builder, mode) {
        override fun addReturnTypeSpecialCase(function: FunctionDescriptor): Boolean = false

        override fun copy(newMode: MangleMode): DescriptorMangleComputer = JKlibDescriptorManglerComputer(builder, mainDetector, newMode)

        private fun isMainFunction(descriptor: FunctionDescriptor): Boolean =
            mainDetector != null && mainDetector.isMain(descriptor)

        override fun FunctionDescriptor.platformSpecificSuffix(): String? =
            if (isMainFunction(this)) source.containingFile.name else null

        override fun PropertyDescriptor.platformSpecificSuffix(): String? {
            // Since LV 1.4 there is a feature PreferJavaFieldOverload which allows to have java and kotlin
            // properties with the same signature on the same level.
            // For more details see JvmPlatformOverloadsSpecificityComparator.kt
            return if (isJavaField) MangleConstant.JAVA_FIELD_SUFFIX else null
        }

        override fun visitModuleDeclaration(descriptor: ModuleDescriptor) {
            // In general, having module descriptor as `containingDeclaration` for regular declaration is considered an error (in JS/Native)
            // because there should be `PackageFragmentDescriptor` in between
            // but on JVM there is `SyntheticJavaPropertyDescriptor` whose parent is a module. So let just skip it.
        }

        override fun mangleTypePlatformSpecific(type: KotlinType, tBuilder: StringBuilder) {
            // Disambiguate between 'double' and '@NotNull java.lang.Double' types in mixed Java/Kotlin class hierarchies
            if (SimpleClassicTypeSystemContext.hasEnhancedNullability(type)) {
                tBuilder.appendSignature(MangleConstant.ENHANCED_NULLABILITY_MARK)
            }
        }
    }

    override fun getExportChecker(compatibleMode: Boolean): KotlinExportChecker<DeclarationDescriptor> = ExportChecker

    override fun getMangleComputer(mode: MangleMode, compatibleMode: Boolean): KotlinMangleComputer<DeclarationDescriptor> =
        JKlibDescriptorManglerComputer(StringBuilder(256), mainDetector, mode)
}

fun String.isKotlinPackage(): Boolean {
    return this == "kotlin" || startsWith("kotlin.")
}

private object JvmTypeFactoryImpl : JvmTypeFactory<JvmType> {
    private val BOOLEAN = JvmType.Primitive(JvmPrimitiveType.BOOLEAN)
    private val CHAR = JvmType.Primitive(JvmPrimitiveType.CHAR)
    private val BYTE = JvmType.Primitive(JvmPrimitiveType.BYTE)
    private val SHORT = JvmType.Primitive(JvmPrimitiveType.SHORT)
    private val INT = JvmType.Primitive(JvmPrimitiveType.INT)
    private val FLOAT = JvmType.Primitive(JvmPrimitiveType.FLOAT)
    private val LONG = JvmType.Primitive(JvmPrimitiveType.LONG)
    private val DOUBLE = JvmType.Primitive(JvmPrimitiveType.DOUBLE)

    override fun boxType(possiblyPrimitiveType: JvmType) =
        when {
            possiblyPrimitiveType is JvmType.Primitive && possiblyPrimitiveType.jvmPrimitiveType != null ->
                createObjectType(
                    JvmClassName.byFqNameWithoutInnerClasses(possiblyPrimitiveType.jvmPrimitiveType!!.wrapperFqName).internalName
                )
            else -> possiblyPrimitiveType
        }

    override fun createFromString(representation: String): JvmType {
        assert(representation.isNotEmpty()) { "empty string as JvmType" }
        val firstChar = representation[0]

        JvmPrimitiveType.values().firstOrNull { it.desc[0] == firstChar }?.let {
            return JvmType.Primitive(it)
        }

        return when (firstChar) {
            'V' -> JvmType.Primitive(null)
            '[' -> JvmType.Array(createFromString(representation.substring(1)))
            else -> {
                assert(firstChar == 'L' && representation.endsWith(';')) {
                    "Type that is not primitive nor array should be Object, but '$representation' was found"
                }

                JvmType.Object(representation.substring(1, representation.length - 1))
            }
        }
    }

    override fun createPrimitiveType(primitiveType: PrimitiveType): JvmType =
        when (primitiveType) {
            PrimitiveType.BOOLEAN -> BOOLEAN
            PrimitiveType.CHAR -> CHAR
            PrimitiveType.BYTE -> BYTE
            PrimitiveType.SHORT -> SHORT
            PrimitiveType.INT -> INT
            PrimitiveType.FLOAT -> FLOAT
            PrimitiveType.LONG -> LONG
            PrimitiveType.DOUBLE -> DOUBLE
        }

    override fun createObjectType(internalName: String): JvmType.Object =
        JvmType.Object(internalName)

    override fun toString(type: JvmType): String =
        when (type) {
            is JvmType.Array -> "[" + toString(type.elementType)
            is JvmType.Primitive -> type.jvmPrimitiveType?.desc ?: "V"
            is JvmType.Object -> "L" + type.internalName + ";"
        }

    override val javaLangClassType: JvmType
        get() = createObjectType("java/lang/Class")

}

internal object TypeMappingConfigurationImpl : TypeMappingConfiguration<JvmType> {
    override fun commonSupertype(types: Collection<KotlinType>): KotlinType {
        throw AssertionError("There should be no intersection type in existing descriptors, but found: " + types.joinToString())
    }

    override fun getPredefinedTypeForClass(classDescriptor: ClassDescriptor): JvmType? = null
    override fun getPredefinedInternalNameForClass(classDescriptor: ClassDescriptor): String? = null

    override fun preprocessType(kotlinType: KotlinType): KotlinType? {
        return null
//        type.
    }

    override fun processErrorType(kotlinType: KotlinType, descriptor: ClassDescriptor) {
        // DO nothing
    }
}

fun StringBuilder.appendErasedType(type: KotlinType) {
    append(type.mapToJvmType())
}

fun KotlinType.mapToJvmType(): JvmType =
    mapType(this, JvmTypeFactoryImpl, TypeMappingMode.DEFAULT, TypeMappingConfigurationImpl, descriptorTypeWriter = null)

fun hasVoidReturnType(descriptor: CallableDescriptor): Boolean {
    if (descriptor is ConstructorDescriptor) return true
    return KotlinBuiltIns.isUnit(descriptor.returnType!!) && !TypeUtils.isNullableType(descriptor.returnType!!)
            && descriptor !is PropertyGetterDescriptor
}

fun FunctionDescriptor.computeJvmDescriptor(withReturnType: Boolean = true, withName: Boolean = true): String = buildString {
    if (withName) {
        append(if (this@computeJvmDescriptor is ConstructorDescriptor) "<init>" else name.asString())
    }

    append("(")

    extensionReceiverParameter?.let {
        appendErasedType(it.type)
    }

    for (parameter in valueParameters) {
        appendErasedType(parameter.type)
    }

    append(")")

    if (withReturnType) {
        if (hasVoidReturnType(this@computeJvmDescriptor)) {
            append("V")
        } else {
            val returnType = returnType!!
            // Workaround for b/447548985
            // Given a class annotated with `@NullMarked`, there will be such difference between K1 and K2 signatures for
            // functions containing primitive types in arguments/return value:
            // ```
            // kotlinjavainterop/NullMarkedClass.getNonNullInteger()I // K2
            // kotlinjavainterop/NullMarkedClass.getNonNullInteger()Ljava/lang/Integer; // K1
            // ```
            // The reason is that in K1 the return type is annotated with `@EnhancedNullability`, but in K2 it isn't.
            // The workaround forces adding `@EnhancedNullability` for such types so that they are always boxed
            // when computing function's signature.
            if (returnType is SimpleType &&
                KotlinBuiltIns.isPrimitiveType(returnType) &&
                !returnType.hasEnhancedNullability() &&
                containingDeclaration.annotations.hasAnnotation(JSPECIFY_NULL_MARKED_ANNOTATION_FQ_NAME)
            ) {
                val newType = returnType.replaceAnnotations(
                    CompositeAnnotations(
                        returnType.annotations,
                        ENHANCED_NULLABILITY_ANNOTATIONS
                    )
                )
                appendErasedType(newType)
            } else {
                appendErasedType(returnType)
            }
        }
    }
}

fun CallableDescriptor.computeJvmSignature(): String? = signatures {
    if (DescriptorUtils.isLocal(this@computeJvmSignature)) return null

    val classDescriptor = containingDeclaration as? ClassDescriptor ?: return null
    if (classDescriptor.name.isSpecial) return null

    signature(
        classDescriptor,
        (original as? FunctionDescriptor ?: return null).computeJvmDescriptor()
    )
}

fun CallableDescriptor.computeJvmSignatureSafe(): String? {
    return try {
        computeJvmSignature()
    } catch (_: Exception) {
        println("ERR: ${name}")
        null
    }
}

fun IrDeclaration.isDeclaredInJava(): Boolean {
    if (origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) return true
    val ownerClass = parentClassOrNull
    if (ownerClass?.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) return true
    return false
}

// If you want to consider Kotlin fake overrides of Java members as "Java-backed":
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrDeclaration.isJavaBackedCallable(): Boolean {
    if (isDeclaredInJava()) return true

    when (this) {
        is IrSimpleFunction -> {
            if (this.isFakeOverride && overriddenSymbols.any { it.owner.isDeclaredInJava() }) return true
        }
        is IrProperty -> {
            // Check accessors and their overrides
            val accs = listOfNotNull(getter, setter)
            if (accs.any { it.isFakeOverride && it.overriddenSymbols.any { s -> s.owner.isDeclaredInJava() } }) return true
        }
    }
    return false
}
