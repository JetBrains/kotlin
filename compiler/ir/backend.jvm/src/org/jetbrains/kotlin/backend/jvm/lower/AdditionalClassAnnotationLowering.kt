/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrEnumEntryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrEnumEntrySymbolImpl
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.lang.annotation.ElementType

internal val additionalClassAnnotationPhase = makeIrFilePhase(
    ::AdditionalClassAnnotationLowering,
    name = "AdditionalClassAnnotation",
    description = "Add Documented, Retention and Target annotations to annotation classes"
)

private class AdditionalClassAnnotationLowering(private val context: JvmBackendContext) : ClassLoweringPass {
    private val jvmTarget = context.state.target

    // TODO: import IR structures from the library?

    private val annotationPackage: IrPackageFragment = IrExternalPackageFragmentImpl.createEmptyExternalPackageFragment(
        context.ir.irModule.descriptor,
        FqName("java.lang.annotation")
    )

    private fun buildAnnotationClass(
        className: String,
        classKind: ClassKind = ClassKind.ANNOTATION_CLASS
    ): IrClass = context.irFactory.buildClass {
        name = Name.identifier(className)
        kind = classKind
    }.apply {
        val irClass = this
        parent = annotationPackage
        annotationPackage.addChild(this)
        thisReceiver = buildValueParameter(this) {
            name = Name.identifier("\$this")
            type = IrSimpleTypeImpl(irClass.symbol, false, emptyList(), emptyList())
        }
    }

    private fun buildAnnotationConstructor(annotationClass: IrClass): IrConstructor = context.irFactory.buildConstructor {
        isPrimary = true
    }.apply {
        parent = annotationClass
        annotationClass.addChild(this)
        returnType = annotationClass.defaultType
    }

    private fun buildEnumEntry(enumClass: IrClass, entryName: String): IrEnumEntry {
        return IrEnumEntryImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB,
            IrEnumEntrySymbolImpl(),
            Name.identifier(entryName)
        ).apply {
            parent = enumClass
            enumClass.addChild(this)
        }
    }

    private val documentedConstructor = buildAnnotationConstructor(buildAnnotationClass("Documented"))

    private val retentionPolicyEnum = buildAnnotationClass("RetentionPolicy", classKind = ClassKind.ENUM_CLASS)
    private val rpSource = buildEnumEntry(retentionPolicyEnum, "SOURCE")
    private val rpClass = buildEnumEntry(retentionPolicyEnum, "CLASS")
    private val rpRuntime = buildEnumEntry(retentionPolicyEnum, "RUNTIME")

    private val retentionConstructor = buildAnnotationConstructor(buildAnnotationClass("Retention")).apply {
        addValueParameter("value", retentionPolicyEnum.defaultType, IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB)
    }

    private val elementTypeEnum = buildAnnotationClass("ElementType", classKind = ClassKind.ENUM_CLASS)
    private val etAnnotationType = buildEnumEntry(elementTypeEnum, "ANNOTATION_TYPE")
    private val etConstructor = buildEnumEntry(elementTypeEnum, "CONSTRUCTOR")
    private val etField = buildEnumEntry(elementTypeEnum, "FIELD")
    private val etLocalVariable = buildEnumEntry(elementTypeEnum, "LOCAL_VARIABLE")
    private val etMethod = buildEnumEntry(elementTypeEnum, "METHOD")
    private val etParameter = buildEnumEntry(elementTypeEnum, "PARAMETER")
    private val etType = buildEnumEntry(elementTypeEnum, "TYPE")
    private val etTypeParameter = buildEnumEntry(elementTypeEnum, "TYPE_PARAMETER")
    private val etTypeUse = buildEnumEntry(elementTypeEnum, "TYPE_USE")

    private val targetConstructor = buildAnnotationConstructor(buildAnnotationClass("Target")).apply {
        addValueParameter("value", elementTypeEnum.defaultType, IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB)
    }


    override fun lower(irClass: IrClass) {
        if (!irClass.isAnnotationClass) return

        generateDocumentedAnnotation(irClass)
        generateRetentionAnnotation(irClass)
        generateTargetAnnotation(irClass)
    }

    private fun generateDocumentedAnnotation(irClass: IrClass) {
        if (!irClass.hasAnnotation(StandardNames.FqNames.mustBeDocumented) ||
            irClass.hasAnnotation(FqName("java.lang.annotation.Documented"))
        ) return

        irClass.annotations +=
            IrConstructorCallImpl.fromSymbolOwner(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, documentedConstructor.returnType, documentedConstructor.symbol, 0
            )
    }

    private val annotationRetentionMap = mapOf(
        KotlinRetention.SOURCE to rpSource,
        KotlinRetention.BINARY to rpClass,
        KotlinRetention.RUNTIME to rpRuntime
    )

    private fun generateRetentionAnnotation(irClass: IrClass) {
        if (irClass.hasAnnotation(FqName("java.lang.annotation.Retention"))) return
        val kotlinRetentionPolicyCall = irClass.getAnnotation(FqName("kotlin.annotation.Retention"))
        val kotlinRetentionPolicyName =
            kotlinRetentionPolicyCall?.getValueArgument(0)?.safeAs<IrGetEnumValue>()?.symbol?.owner?.name?.asString()
        val kotlinRetentionPolicy = kotlinRetentionPolicyName?.let { KotlinRetention.valueOf(it) }
        val javaRetentionPolicy = kotlinRetentionPolicy?.let { annotationRetentionMap[it] } ?: rpRuntime

        irClass.annotations +=
            IrConstructorCallImpl.fromSymbolOwner(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, retentionConstructor.returnType, retentionConstructor.symbol, 0
            ).apply {
                putValueArgument(
                    0,
                    IrGetEnumValueImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET, retentionPolicyEnum.defaultType, javaRetentionPolicy.symbol
                    )
                )
            }
    }

    private val jvm6TargetMap = mutableMapOf(
        KotlinTarget.CLASS to etType,
        KotlinTarget.ANNOTATION_CLASS to etAnnotationType,
        KotlinTarget.CONSTRUCTOR to etConstructor,
        KotlinTarget.LOCAL_VARIABLE to etLocalVariable,
        KotlinTarget.FUNCTION to etMethod,
        KotlinTarget.PROPERTY_GETTER to etMethod,
        KotlinTarget.PROPERTY_SETTER to etMethod,
        KotlinTarget.FIELD to etField,
        KotlinTarget.VALUE_PARAMETER to etParameter
    )

    private val jvm8TargetMap = jvm6TargetMap + mutableMapOf(
        KotlinTarget.TYPE_PARAMETER to etTypeParameter,
        KotlinTarget.TYPE to etTypeUse
    )

    private val annotationTargetMaps: Map<JvmTarget, Map<KotlinTarget, IrEnumEntry>> =
        JvmTarget.values().associate { target ->
            target to (if (target == JvmTarget.JVM_1_6) jvm6TargetMap else jvm8TargetMap)
        }

    private fun generateTargetAnnotation(irClass: IrClass) {
        if (irClass.hasAnnotation(JvmAnnotationNames.TARGET_ANNOTATION)) return
        val annotationTargetMap = annotationTargetMaps[jvmTarget]
            ?: throw AssertionError("No annotation target map for JVM target $jvmTarget")

        val targets = irClass.applicableTargetSet() ?: return
        val javaTargets = targets.mapNotNullTo(HashSet()) { annotationTargetMap[it] }.sortedBy {
            ElementType.valueOf(it.symbol.owner.name.asString())
        }

        val vararg = IrVarargImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            type = context.irBuiltIns.arrayClass.typeWith(elementTypeEnum.defaultType),
            varargElementType = elementTypeEnum.defaultType
        )
        for (target in javaTargets) {
            vararg.elements.add(
                IrGetEnumValueImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, elementTypeEnum.defaultType, target.symbol
                )
            )
        }

        irClass.annotations +=
            IrConstructorCallImpl.fromSymbolOwner(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, targetConstructor.returnType, targetConstructor.symbol, 0
            ).apply {
                putValueArgument(0, vararg)
            }
        // TODO
    }
}

// To be generalized to IrMemberAccessExpression as soon as properties get symbols.
private fun IrConstructorCall.getValueArgument(name: Name): IrExpression? {
    val index = symbol.owner.valueParameters.find { it.name == name }?.index ?: return null
    return getValueArgument(index)
}

// Copied and modified from AnnotationChecker.kt

private val TARGET_ALLOWED_TARGETS = Name.identifier("allowedTargets")

private fun IrClass.applicableTargetSet(): Set<KotlinTarget>? {
    val targetEntry = getAnnotation(StandardNames.FqNames.target) ?: return null
    return loadAnnotationTargets(targetEntry)
}

private fun loadAnnotationTargets(targetEntry: IrConstructorCall): Set<KotlinTarget>? {
    val valueArgument = targetEntry.getValueArgument(TARGET_ALLOWED_TARGETS)
            as? IrVararg ?: return null
    return valueArgument.elements.filterIsInstance<IrGetEnumValue>().mapNotNull {
        KotlinTarget.valueOrNull(it.symbol.owner.name.asString())
    }.toSet()
}
