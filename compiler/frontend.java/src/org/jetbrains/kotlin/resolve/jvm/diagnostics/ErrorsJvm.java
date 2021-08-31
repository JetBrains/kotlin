/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.diagnostics;

import com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.config.LanguageFeature;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.Named;
import org.jetbrains.kotlin.diagnostics.*;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.types.KotlinType;

import static org.jetbrains.kotlin.diagnostics.PositioningStrategies.*;
import static org.jetbrains.kotlin.diagnostics.Severity.ERROR;
import static org.jetbrains.kotlin.diagnostics.Severity.WARNING;

public interface ErrorsJvm {
    DiagnosticFactory1<PsiElement, ConflictingJvmDeclarationsData> CONFLICTING_JVM_DECLARATIONS =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);

    DiagnosticFactory1<PsiElement, ConflictingJvmDeclarationsData> ACCIDENTAL_OVERRIDE =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory1<PsiElement, ConflictingJvmDeclarationsData> CONFLICTING_INHERITED_JVM_DECLARATIONS =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);

    DiagnosticFactory0<KtDeclaration> OVERRIDE_CANNOT_BE_STATIC = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtDeclaration> JVM_STATIC_NOT_IN_OBJECT_OR_CLASS_COMPANION = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtDeclaration> JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtDeclaration> JVM_STATIC_ON_NON_PUBLIC_MEMBER = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtDeclaration> JVM_STATIC_IN_INTERFACE_1_6 = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtDeclaration> JVM_STATIC_ON_CONST_OR_JVM_FIELD = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtDeclaration> JVM_STATIC_ON_EXTERNAL_IN_INTERFACE = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);

    DiagnosticFactory0<PsiElement> INAPPLICABLE_JVM_NAME = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> ILLEGAL_JVM_NAME = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<KtAnnotationEntry, String> INAPPLICABLE_JVM_FIELD = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtAnnotationEntry, String> INAPPLICABLE_JVM_FIELD_WARNING = DiagnosticFactory1.create(WARNING);

    DiagnosticFactory0<KtAnnotationEntry> JVM_SYNTHETIC_ON_DELEGATE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtAnnotationEntry> STRICTFP_ON_CLASS = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory0<KtAnnotationEntry> VOLATILE_ON_VALUE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> VOLATILE_ON_DELEGATE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> SYNCHRONIZED_ON_ABSTRACT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtElement> SYNCHRONIZED_ON_INLINE = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<PsiElement> SYNCHRONIZED_ON_SUSPEND = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<KtAnnotationEntry> SYNCHRONIZED_IN_INTERFACE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtAnnotationEntry> OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<KtAnnotationEntry> OVERLOADS_ABSTRACT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> OVERLOADS_INTERFACE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> OVERLOADS_PRIVATE = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<KtAnnotationEntry> OVERLOADS_LOCAL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactoryForDeprecation0<KtAnnotationEntry> OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR = DiagnosticFactoryForDeprecation0.create(LanguageFeature.ProhibitJvmOverloadsOnConstructorsOfAnnotationClasses);
    DiagnosticFactory0<KtAnnotationEntry> OVERLOADS_ANNOTATION_MANGLED_FUNCTION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> OVERLOADS_ANNOTATION_HIDDEN_CONSTRUCTOR = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtDeclaration> EXTERNAL_DECLARATION_CANNOT_BE_ABSTRACT = DiagnosticFactory0.create(ERROR, ABSTRACT_MODIFIER);
    DiagnosticFactory0<KtDeclaration> EXTERNAL_DECLARATION_CANNOT_HAVE_BODY = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtDeclaration> EXTERNAL_DECLARATION_IN_INTERFACE = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtDeclaration> EXTERNAL_DECLARATION_CANNOT_BE_INLINED = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);

    DiagnosticFactory0<KtExpression> POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtAnnotationEntry, FqName> DEPRECATED_JAVA_ANNOTATION = DiagnosticFactory1.create(WARNING);

    DiagnosticFactory0<KtAnnotationEntry> NON_SOURCE_REPEATED_ANNOTATION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> REPEATED_ANNOTATION_TARGET6 = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<KtAnnotationEntry, FqName, FqName> REPEATED_ANNOTATION_WITH_CONTAINER = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory2<KtAnnotationEntry, FqName, FqName> REPEATABLE_CONTAINER_MUST_HAVE_VALUE_ARRAY = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<KtAnnotationEntry, FqName, FqName> REPEATABLE_CONTAINER_MUST_HAVE_VALUE_ARRAY_ERROR = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<KtAnnotationEntry, FqName, Named> REPEATABLE_CONTAINER_HAS_NON_DEFAULT_PARAMETER = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<KtAnnotationEntry, FqName, Named> REPEATABLE_CONTAINER_HAS_NON_DEFAULT_PARAMETER_ERROR = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory4<KtAnnotationEntry, FqName, String, FqName, String> REPEATABLE_CONTAINER_HAS_SHORTER_RETENTION = DiagnosticFactory4.create(WARNING);
    DiagnosticFactory4<KtAnnotationEntry, FqName, String, FqName, String> REPEATABLE_CONTAINER_HAS_SHORTER_RETENTION_ERROR = DiagnosticFactory4.create(ERROR);
    DiagnosticFactory2<KtAnnotationEntry, FqName, FqName> REPEATABLE_CONTAINER_TARGET_SET_NOT_A_SUBSET = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<KtAnnotationEntry, FqName, FqName> REPEATABLE_CONTAINER_TARGET_SET_NOT_A_SUBSET_ERROR = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> REPEATABLE_ANNOTATION_HAS_NESTED_CLASS_NAMED_CONTAINER = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<KtAnnotationEntry> REPEATABLE_ANNOTATION_HAS_NESTED_CLASS_NAMED_CONTAINER_ERROR = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory1<KtAnnotationEntry, FqName> ANNOTATION_IS_NOT_APPLICABLE_TO_MULTIFILE_CLASSES = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<KtAnnotationEntry> JVM_PACKAGE_NAME_CANNOT_BE_EMPTY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> JVM_PACKAGE_NAME_MUST_BE_VALID_NAME = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> JVM_PACKAGE_NAME_NOT_SUPPORTED_IN_FILES_WITH_CLASSES = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtDeclaration> STATE_IN_MULTIFILE_CLASS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> NOT_ALL_MULTIFILE_CLASS_PARTS_ARE_JVM_SYNTHETIC = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> NO_REFLECTION_IN_CLASS_PATH = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory2<PsiElement, KotlinType, KotlinType> JAVA_CLASS_ON_COMPANION = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<KtExpression, KotlinType, KotlinType> JAVA_TYPE_MISMATCH = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory2<PsiElement, String, String> DUPLICATE_CLASS_NAMES = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory0<PsiElement> UPPER_BOUND_CANNOT_BE_ARRAY = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<PsiElement, String> SUPER_CALL_WITH_DEFAULT_PARAMETERS = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<KtExpression> WHEN_ENUM_CAN_BE_NULL_IN_JAVA = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory3<PsiElement, DeclarationDescriptor, DeclarationDescriptor, String> TARGET6_INTERFACE_INHERITANCE = DiagnosticFactory3.create(ERROR);

    DiagnosticFactoryForDeprecation0<PsiElement> DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET = DiagnosticFactoryForDeprecation0.create(LanguageFeature.DefaultMethodsCallFromJava6TargetError);
    DiagnosticFactoryForDeprecation0<PsiElement> INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET = DiagnosticFactoryForDeprecation0.create(LanguageFeature.DefaultMethodsCallFromJava6TargetError);

    DiagnosticFactory2<PsiElement, String, String> INLINE_FROM_HIGHER_PLATFORM = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<PsiElement, String, String> INLINE_FROM_HIGHER_PLATFORM_WARNING = DiagnosticFactory2.create(WARNING);

    DiagnosticFactory1<PsiElement, String> JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> JAVA_MODULE_DOES_NOT_READ_UNNAMED_MODULE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<PsiElement, String, String> JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory0<KtExpression> API_VERSION_IS_AT_LEAST_ARGUMENT_SHOULD_BE_CONSTANT = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory0<KtExpression> ASSIGNMENT_TO_ARRAY_LOOP_VARIABLE = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory0<PsiElement> JVM_DEFAULT_NOT_IN_INTERFACE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, String> JVM_DEFAULT_IN_JVM6_TARGET = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<KtDeclaration> JVM_DEFAULT_REQUIRED_FOR_OVERRIDE = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory1<KtElement, String> JVM_DEFAULT_IN_DECLARATION = DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<KtDeclaration> JVM_DEFAULT_THROUGH_INHERITANCE = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<PsiElement> USAGE_OF_JVM_DEFAULT_THROUGH_SUPER_CALL = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> LOCAL_JVM_RECORD = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> NON_FINAL_JVM_RECORD = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> ENUM_JVM_RECORD = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> JVM_RECORD_WITHOUT_PRIMARY_CONSTRUCTOR_PARAMETERS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> JVM_RECORD_NOT_VAL_PARAMETER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> JVM_RECORD_NOT_LAST_VARARG_PARAMETER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, KotlinType> JVM_RECORD_EXTENDS_CLASS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> JVM_RECORD_REQUIRES_JDK15 = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> INNER_JVM_RECORD = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> FIELD_IN_JVM_RECORD = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> DELEGATION_BY_IN_JVM_RECORD = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> NON_DATA_CLASS_JVM_RECORD = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> ILLEGAL_JAVA_LANG_RECORD_SUPERTYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> JVM_RECORDS_ILLEGAL_BYTECODE_TARGET = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtDeclaration> NON_JVM_DEFAULT_OVERRIDES_JAVA_DEFAULT = DiagnosticFactory0.create(WARNING, DECLARATION_SIGNATURE);

    DiagnosticFactory0<KtAnnotationEntry> EXPLICIT_METADATA_IS_DISALLOWED = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory2<KtElement, KotlinType, KotlinType> NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS
            = DiagnosticFactory2.create(WARNING);

    DiagnosticFactory2<KtTypeReference, KotlinType, KotlinType> UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS
            = DiagnosticFactory2.create(WARNING);

    DiagnosticFactory3<KtElement, KotlinType, KotlinType, ClassifierDescriptor> UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_BASED_ON_JAVA_ANNOTATIONS
            = DiagnosticFactory3.create(WARNING);

    DiagnosticFactory1<KtElement, KotlinType> NULLABLE_TYPE_PARAMETER_AGAINST_NOT_NULL_TYPE_PARAMETER
            = DiagnosticFactory1.create(WARNING);

    DiagnosticFactory1<KtElement, KotlinType> RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS
            = DiagnosticFactory1.create(WARNING);

    DiagnosticFactory1<KtAnnotationEntry, String> ANNOTATION_TARGETS_NON_EXISTENT_ACCESSOR = DiagnosticFactory1.create(WARNING);

    DiagnosticFactory1<PsiElement, String> SUSPENSION_POINT_INSIDE_MONITOR = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, CallableDescriptor> SUSPENSION_POINT_INSIDE_CRITICAL_SECTION = DiagnosticFactory1.create(ERROR);

    DiagnosticFactoryForDeprecation0<PsiElement> CONCURRENT_HASH_MAP_CONTAINS_OPERATOR = DiagnosticFactoryForDeprecation0.create(LanguageFeature.ProhibitConcurrentHashMapContains);

    DiagnosticFactory0<KtNamedFunction> TAILREC_WITH_DEFAULTS = DiagnosticFactory0.create(WARNING, DECLARATION_SIGNATURE);

    DiagnosticFactoryForDeprecation0<PsiElement> SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL = DiagnosticFactoryForDeprecation0.create(LanguageFeature.ProhibitSpreadOnSignaturePolymorphicCall);

    DiagnosticFactory0<PsiElement> FUNCTION_DELEGATE_MEMBER_NAME_CLASH = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory2<KtDeclaration, CallableDescriptor, CallableDescriptor> EXPLICIT_OVERRIDE_REQUIRED_IN_COMPATIBILITY_MODE = DiagnosticFactory2.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory3<KtDeclaration, CallableDescriptor, CallableDescriptor, String> EXPLICIT_OVERRIDE_REQUIRED_IN_MIXED_MODE = DiagnosticFactory3.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);

    DiagnosticFactory1<PsiElement, String> DANGEROUS_CHARACTERS = DiagnosticFactory1.create(WARNING);

    DiagnosticFactory0<PsiElement> VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> JVM_INLINE_WITHOUT_VALUE_CLASS = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> TYPEOF_SUSPEND_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> TYPEOF_EXTENSION_FUNCTION_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> TYPEOF_ANNOTATED_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, String> TYPEOF_NON_REIFIED_TYPE_PARAMETER_WITH_RECURSIVE_BOUND = DiagnosticFactory1.create(ERROR);

    @SuppressWarnings("UnusedDeclaration")
    Object _initializer = new Object() {
        {
            Errors.Initializer.initializeFactoryNames(ErrorsJvm.class);
        }
    };
}
