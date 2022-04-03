/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageFeature.*
import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.DiagnosticList
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.PositioningStrategy
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression

@Suppress("UNUSED_VARIABLE", "LocalVariableName", "ClassName", "unused")
@OptIn(PrivateForInline::class)
object JVM_DIAGNOSTICS_LIST : DiagnosticList("FirJvmErrors") {
    val DECLARATIONS by object : DiagnosticGroup("Declarations") {
        val CONFLICTING_JVM_DECLARATIONS by error<PsiElement>()

        val OVERRIDE_CANNOT_BE_STATIC by error<PsiElement>()
        val JVM_STATIC_NOT_IN_OBJECT_OR_CLASS_COMPANION by error<PsiElement>(PositioningStrategy.DECLARATION_SIGNATURE)
        val JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION by error<PsiElement>(PositioningStrategy.DECLARATION_SIGNATURE)
        val JVM_STATIC_ON_NON_PUBLIC_MEMBER by error<PsiElement>(PositioningStrategy.DECLARATION_SIGNATURE)
        val JVM_STATIC_ON_CONST_OR_JVM_FIELD by error<PsiElement>(PositioningStrategy.DECLARATION_SIGNATURE)
        val JVM_STATIC_ON_EXTERNAL_IN_INTERFACE by error<PsiElement>(PositioningStrategy.DECLARATION_SIGNATURE)

        val INAPPLICABLE_JVM_NAME by error<PsiElement>()
        val ILLEGAL_JVM_NAME by error<PsiElement>()

        val FUNCTION_DELEGATE_MEMBER_NAME_CLASH by error<PsiElement>(PositioningStrategy.DECLARATION_NAME)

        val VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION by error<PsiElement>()
        val JVM_INLINE_WITHOUT_VALUE_CLASS by error<PsiElement>()
    }

    val TYPES by object : DiagnosticGroup("Types") {
        val JAVA_TYPE_MISMATCH by error<KtExpression> {
            parameter<ConeKotlinType>("expectedType")
            parameter<ConeKotlinType>("actualType")
        }
    }

    val TYPE_PARAMETERS by object : DiagnosticGroup("Type parameters") {
        val UPPER_BOUND_CANNOT_BE_ARRAY by error<PsiElement>()
    }

    val ANNOTATIONS by object : DiagnosticGroup("annotations") {
        val STRICTFP_ON_CLASS by error<KtAnnotationEntry>()
        val VOLATILE_ON_VALUE by error<KtAnnotationEntry>()
        val VOLATILE_ON_DELEGATE by error<KtAnnotationEntry>()
        val SYNCHRONIZED_ON_ABSTRACT by error<KtAnnotationEntry>()
        val SYNCHRONIZED_IN_INTERFACE by error<KtAnnotationEntry>()
        val SYNCHRONIZED_ON_INLINE by warning<KtAnnotationEntry>()
        val SYNCHRONIZED_ON_SUSPEND by deprecationError<KtAnnotationEntry>(SynchronizedSuspendError)
        val OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS by warning<KtAnnotationEntry>()
        val OVERLOADS_ABSTRACT by error<KtAnnotationEntry>()
        val OVERLOADS_INTERFACE by error<KtAnnotationEntry>()
        val OVERLOADS_LOCAL by error<KtAnnotationEntry>()
        val OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR by deprecationError<KtAnnotationEntry>(
            ProhibitJvmOverloadsOnConstructorsOfAnnotationClasses
        )
        val OVERLOADS_PRIVATE by warning<KtAnnotationEntry>()
        val DEPRECATED_JAVA_ANNOTATION by warning<KtAnnotationEntry>() {
            parameter<FqName>("kotlinName")
        }

        val JVM_PACKAGE_NAME_CANNOT_BE_EMPTY by error<KtAnnotationEntry>()
        val JVM_PACKAGE_NAME_MUST_BE_VALID_NAME by error<KtAnnotationEntry>()
        val JVM_PACKAGE_NAME_NOT_SUPPORTED_IN_FILES_WITH_CLASSES by error<KtAnnotationEntry>()

        val POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION by error<KtExpression>()
    }

    val SUPER by object : DiagnosticGroup("Super") {
        val SUPER_CALL_WITH_DEFAULT_PARAMETERS by error<PsiElement>() {
            parameter<String>("name")
        }
        val INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER by error<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED)
    }

    val RECORDS by object : DiagnosticGroup("JVM Records") {
        val LOCAL_JVM_RECORD by error<PsiElement>()
        val NON_FINAL_JVM_RECORD by error<PsiElement>(PositioningStrategy.NON_FINAL_MODIFIER_OR_NAME)
        val ENUM_JVM_RECORD by error<PsiElement>(PositioningStrategy.ENUM_MODIFIER)
        val JVM_RECORD_WITHOUT_PRIMARY_CONSTRUCTOR_PARAMETERS by error<PsiElement>()
        val NON_DATA_CLASS_JVM_RECORD by error<PsiElement>()
        val JVM_RECORD_NOT_VAL_PARAMETER by error<PsiElement>()
        val JVM_RECORD_NOT_LAST_VARARG_PARAMETER by error<PsiElement>()
        val INNER_JVM_RECORD by error<PsiElement>(PositioningStrategy.INNER_MODIFIER)
        val FIELD_IN_JVM_RECORD by error<PsiElement>()
        val DELEGATION_BY_IN_JVM_RECORD by error<PsiElement>()
        val JVM_RECORD_EXTENDS_CLASS by error<PsiElement>(PositioningStrategy.ACTUAL_DECLARATION_NAME) {
            parameter<ConeKotlinType>("superType")
        }
        val ILLEGAL_JAVA_LANG_RECORD_SUPERTYPE by error<PsiElement>()
    }

    val JVM_DEFAULT by object : DiagnosticGroup("JVM Default") {
        val JVM_DEFAULT_NOT_IN_INTERFACE by error<PsiElement>()
        val JVM_DEFAULT_IN_JVM6_TARGET by error<PsiElement> {
            parameter<String>("annotation")
        }
        val JVM_DEFAULT_REQUIRED_FOR_OVERRIDE by error<KtDeclaration>(PositioningStrategy.DECLARATION_SIGNATURE)
        val JVM_DEFAULT_IN_DECLARATION by error<KtElement>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT) {
            parameter<String>("annotation")
        }
        val JVM_DEFAULT_WITH_COMPATIBILITY_IN_DECLARATION by error<KtElement>()
        val JVM_DEFAULT_WITH_COMPATIBILITY_NOT_ON_INTERFACE by error<KtElement>()
        val NON_JVM_DEFAULT_OVERRIDES_JAVA_DEFAULT by warning<KtDeclaration>(PositioningStrategy.DECLARATION_SIGNATURE)
    }

    val EXTERNAL_DECLARATION by object : DiagnosticGroup("External Declaration") {
        val EXTERNAL_DECLARATION_CANNOT_BE_ABSTRACT by error<KtDeclaration>(PositioningStrategy.ABSTRACT_MODIFIER)
        val EXTERNAL_DECLARATION_CANNOT_HAVE_BODY by error<KtDeclaration>(PositioningStrategy.DECLARATION_SIGNATURE)
        val EXTERNAL_DECLARATION_IN_INTERFACE by error<KtDeclaration>(PositioningStrategy.DECLARATION_SIGNATURE)
        val EXTERNAL_DECLARATION_CANNOT_BE_INLINED by error<KtDeclaration>(PositioningStrategy.DECLARATION_SIGNATURE)
    }

    val REPEATABLE by object : DiagnosticGroup("Repeatable Annotations") {
        val NON_SOURCE_REPEATED_ANNOTATION by error<KtAnnotationEntry>()
        val REPEATED_ANNOTATION_TARGET6 by error<KtAnnotationEntry>()
        val REPEATED_ANNOTATION_WITH_CONTAINER by error<KtAnnotationEntry> {
            parameter<ClassId>("name")
            parameter<ClassId>("explicitContainerName")
        }

        val REPEATABLE_CONTAINER_MUST_HAVE_VALUE_ARRAY by deprecationError<KtAnnotationEntry>(RepeatableAnnotationContainerConstraints) {
            parameter<ClassId>("container")
            parameter<ClassId>("annotation")
        }
        val REPEATABLE_CONTAINER_HAS_NON_DEFAULT_PARAMETER by deprecationError<KtAnnotationEntry>(RepeatableAnnotationContainerConstraints) {
            parameter<ClassId>("container")
            parameter<Name>("nonDefault")
        }
        val REPEATABLE_CONTAINER_HAS_SHORTER_RETENTION by deprecationError<KtAnnotationEntry>(RepeatableAnnotationContainerConstraints) {
            parameter<ClassId>("container")
            parameter<String>("retention")
            parameter<ClassId>("annotation")
            parameter<String>("annotationRetention")
        }
        val REPEATABLE_CONTAINER_TARGET_SET_NOT_A_SUBSET by deprecationError<KtAnnotationEntry>(RepeatableAnnotationContainerConstraints) {
            parameter<ClassId>("container")
            parameter<ClassId>("annotation")
        }
        val REPEATABLE_ANNOTATION_HAS_NESTED_CLASS_NAMED_CONTAINER by deprecationError<KtAnnotationEntry>(
            RepeatableAnnotationContainerConstraints
        )
    }

    val SUSPENSION_POINT by object : DiagnosticGroup("Suspension Point") {
        val SUSPENSION_POINT_INSIDE_CRITICAL_SECTION by error<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<FirCallableSymbol<*>>("function")
        }
    }

    val MISC by object : DiagnosticGroup("Misc") {
        val INAPPLICABLE_JVM_FIELD by error<KtAnnotationEntry> {
            parameter<String>("message")
        }
        val INAPPLICABLE_JVM_FIELD_WARNING by warning<KtAnnotationEntry> {
            parameter<String>("message")
        }
        val JVM_SYNTHETIC_ON_DELEGATE by error<KtAnnotationEntry>()
        val DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET by deprecationError<PsiElement>(
            DefaultMethodsCallFromJava6TargetError,
            PositioningStrategy.REFERENCE_BY_QUALIFIED
        )
        val INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET by deprecationError<PsiElement>(
            DefaultMethodsCallFromJava6TargetError,
            PositioningStrategy.REFERENCE_BY_QUALIFIED
        )
        val SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val CONCURRENT_HASH_MAP_CONTAINS_OPERATOR by deprecationError<PsiElement>(ProhibitConcurrentHashMapContains)
        val SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL by deprecationError<PsiElement>(
            ProhibitSpreadOnSignaturePolymorphicCall,
            PositioningStrategy.SPREAD_OPERATOR
        )
        val JAVA_SAM_INTERFACE_CONSTRUCTOR_REFERENCE by error<PsiElement>()
    }
}
