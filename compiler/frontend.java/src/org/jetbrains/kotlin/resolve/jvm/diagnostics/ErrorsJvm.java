/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory2;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetAnnotationEntry;
import org.jetbrains.kotlin.psi.JetDeclaration;
import org.jetbrains.kotlin.psi.JetElement;
import org.jetbrains.kotlin.psi.JetExpression;

import static org.jetbrains.kotlin.diagnostics.PositioningStrategies.*;
import static org.jetbrains.kotlin.diagnostics.Severity.ERROR;
import static org.jetbrains.kotlin.diagnostics.Severity.WARNING;

public interface ErrorsJvm {
    DiagnosticFactory1<PsiElement, ConflictingJvmDeclarationsData> CONFLICTING_JVM_DECLARATIONS =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);

    DiagnosticFactory1<PsiElement, ConflictingJvmDeclarationsData> ACCIDENTAL_OVERRIDE =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);

    DiagnosticFactory0<JetDeclaration> OVERRIDE_CANNOT_BE_STATIC = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<JetDeclaration> PLATFORM_STATIC_NOT_IN_OBJECT = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);

    DiagnosticFactory0<JetDeclaration> OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS = DiagnosticFactory0.create(WARNING, DECLARATION_SIGNATURE);
    DiagnosticFactory0<JetDeclaration> OVERLOADS_ABSTRACT = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<JetDeclaration> OVERLOADS_PRIVATE = DiagnosticFactory0.create(WARNING, DECLARATION_SIGNATURE);

    DiagnosticFactory0<JetDeclaration> NATIVE_DECLARATION_CANNOT_BE_ABSTRACT = DiagnosticFactory0.create(ERROR, ABSTRACT_MODIFIER);
    DiagnosticFactory0<JetDeclaration> NATIVE_DECLARATION_CANNOT_HAVE_BODY = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<JetDeclaration> NATIVE_DECLARATION_IN_TRAIT = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<JetDeclaration> NATIVE_DECLARATION_CANNOT_BE_INLINED = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);

    DiagnosticFactory0<JetExpression>              POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetElement>                 DEPRECATED_ANNOTATION_METHOD_CALL      = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory1<JetAnnotationEntry, FqName> DEPRECATED_JAVA_ANNOTATION = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory0<JetAnnotationEntry>         NON_SOURCE_REPEATED_ANNOTATION = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<JetElement> TRAIT_CANT_CALL_DEFAULT_METHOD_VIA_SUPER = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<JetElement> INAPPLICABLE_PUBLIC_FIELD = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<JetElement> NO_REFLECTION_IN_CLASS_PATH = DiagnosticFactory0.create(WARNING);

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
        };
    }

    DiagnosticFactory2<JetElement, NullabilityInformationSource, NullabilityInformationSource> NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS = DiagnosticFactory2.create(WARNING);

    DiagnosticFactory0<JetExpression> WHEN_ENUM_CAN_BE_NULL_IN_JAVA = DiagnosticFactory0.create(WARNING);

    @SuppressWarnings("UnusedDeclaration")
    Object _initializer = new Object() {
        {
            Errors.Initializer.initializeFactoryNames(ErrorsJvm.class);
        }
    };
}
