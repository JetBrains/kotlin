/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.jvm.diagnostics;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
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
    DiagnosticFactory0<KtDeclaration> JVM_STATIC_NOT_IN_OBJECT = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
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

    DiagnosticFactory0<KtDeclaration> EXTERNAL_DECLARATION_CANNOT_BE_ABSTRACT = DiagnosticFactory0.create(ERROR, ABSTRACT_MODIFIER);
    DiagnosticFactory0<KtDeclaration> EXTERNAL_DECLARATION_CANNOT_HAVE_BODY = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtDeclaration> EXTERNAL_DECLARATION_IN_INTERFACE = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtDeclaration> EXTERNAL_DECLARATION_CANNOT_BE_INLINED = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);

    DiagnosticFactory0<KtExpression> POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtAnnotationEntry, FqName> DEPRECATED_JAVA_ANNOTATION = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory0<KtAnnotationEntry> NON_SOURCE_REPEATED_ANNOTATION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtAnnotationEntry, FqName> ANNOTATION_IS_NOT_APPLICABLE_TO_MULTIFILE_CLASSES = DiagnosticFactory1.create(ERROR);

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
    DiagnosticFactory0<PsiElement> INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory2<PsiElement, String, String> INLINE_FROM_HIGHER_PLATFORM = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory1<PsiElement, String> JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> JAVA_MODULE_DOES_NOT_READ_UNNAMED_MODULE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<PsiElement, String, String> JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE = DiagnosticFactory2.create(ERROR);

    enum NullabilityInformationSource {
        KOTLIN {
            @NotNull
            @Override
            public String toString() {
                return "Kotlin";
            }
        },
        JAVA {
            @NotNull
            @Override
            public String toString() {
                return "Java";
            }
        }
    }

    DiagnosticFactory2<KtElement, NullabilityInformationSource, NullabilityInformationSource> NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS
            = DiagnosticFactory2.create(WARNING);

    @SuppressWarnings("UnusedDeclaration")
    Object _initializer = new Object() {
        {
            Errors.Initializer.initializeFactoryNames(ErrorsJvm.class);
        }
    };
}
