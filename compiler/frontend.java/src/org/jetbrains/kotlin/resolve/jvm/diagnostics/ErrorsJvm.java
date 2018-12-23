/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.diagnostics;

import com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.diagnostics.*;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtAnnotationEntry;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtExpression;
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

    DiagnosticFactory0<PsiElement> INAPPLICABLE_JVM_NAME = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> ILLEGAL_JVM_NAME = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<KtAnnotationEntry, String> INAPPLICABLE_JVM_FIELD = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<KtAnnotationEntry> JVM_SYNTHETIC_ON_DELEGATE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtAnnotationEntry> STRICTFP_ON_CLASS = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory0<KtAnnotationEntry> VOLATILE_ON_VALUE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> VOLATILE_ON_DELEGATE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> SYNCHRONIZED_ON_ABSTRACT = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtAnnotationEntry> OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<KtAnnotationEntry> OVERLOADS_ABSTRACT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> OVERLOADS_INTERFACE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> OVERLOADS_PRIVATE = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<KtAnnotationEntry> OVERLOADS_LOCAL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR_WARNING = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<KtAnnotationEntry> OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR = DiagnosticFactory0.create(ERROR);


    DiagnosticFactory0<KtDeclaration> EXTERNAL_DECLARATION_CANNOT_BE_ABSTRACT = DiagnosticFactory0.create(ERROR, ABSTRACT_MODIFIER);
    DiagnosticFactory0<KtDeclaration> EXTERNAL_DECLARATION_CANNOT_HAVE_BODY = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtDeclaration> EXTERNAL_DECLARATION_IN_INTERFACE = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtDeclaration> EXTERNAL_DECLARATION_CANNOT_BE_INLINED = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);

    DiagnosticFactory0<KtExpression> POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtAnnotationEntry, FqName> DEPRECATED_JAVA_ANNOTATION = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory0<KtAnnotationEntry> NON_SOURCE_REPEATED_ANNOTATION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtAnnotationEntry, FqName> ANNOTATION_IS_NOT_APPLICABLE_TO_MULTIFILE_CLASSES = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<KtAnnotationEntry> JVM_PACKAGE_NAME_NOT_SUPPORTED_IN_MULTIFILE_CLASSES = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> JVM_PACKAGE_NAME_CANNOT_BE_EMPTY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> JVM_PACKAGE_NAME_MUST_BE_VALID_NAME = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> JVM_PACKAGE_NAME_NOT_SUPPORTED_IN_FILES_WITH_CLASSES = DiagnosticFactory0.create(ERROR);

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

    DiagnosticFactory0<PsiElement> DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<PsiElement> DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET_ERROR = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<PsiElement> INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET_ERROR = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory2<PsiElement, String, String> INLINE_FROM_HIGHER_PLATFORM = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory1<PsiElement, String> JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> JAVA_MODULE_DOES_NOT_READ_UNNAMED_MODULE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<PsiElement, String, String> JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory0<KtExpression> API_VERSION_IS_AT_LEAST_ARGUMENT_SHOULD_BE_CONSTANT = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory0<KtExpression> ASSIGNMENT_TO_ARRAY_LOOP_VARIABLE = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory0<PsiElement> JVM_DEFAULT_NOT_IN_INTERFACE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> JVM_DEFAULT_IN_JVM6_TARGET = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtDeclaration> JVM_DEFAULT_REQUIRED_FOR_OVERRIDE = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtDeclaration> JVM_DEFAULT_IN_DECLARATION = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtDeclaration> JVM_DEFAULT_THROUGH_INHERITANCE = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<PsiElement> USAGE_OF_JVM_DEFAULT_THROUGH_SUPER_CALL = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtDeclaration> NON_JVM_DEFAULT_OVERRIDES_JAVA_DEFAULT = DiagnosticFactory0.create(WARNING, DECLARATION_SIGNATURE);

    DiagnosticFactory0<KtAnnotationEntry> EXPLICIT_METADATA_IS_DISALLOWED = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory2<KtElement, KotlinType, KotlinType> NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS
            = DiagnosticFactory2.create(WARNING);

    DiagnosticFactory1<KtElement, KotlinType> RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS
            = DiagnosticFactory1.create(WARNING);

    DiagnosticFactory1<KtAnnotationEntry, String> ANNOTATION_TARGETS_NON_EXISTENT_ACCESSOR = DiagnosticFactory1.create(WARNING);

    DiagnosticFactory1<PsiElement, String> SUSPENSION_POINT_INSIDE_MONITOR = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, CallableDescriptor> SUSPENSION_POINT_INSIDE_SYNCHRONIZED = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<PsiElement> CONCURRENT_HASH_MAP_CONTAINS_OPERATOR = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<PsiElement> CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR = DiagnosticFactory0.create(ERROR);

    @SuppressWarnings("UnusedDeclaration")
    Object _initializer = new Object() {
        {
            Errors.Initializer.initializeFactoryNames(ErrorsJvm.class);
        }
    };
}
