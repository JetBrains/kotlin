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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap;
import org.jetbrains.kotlin.diagnostics.rendering.Renderers;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.renderer.Renderer;

public class DefaultErrorMessagesJvm implements DefaultErrorMessages.Extension {

    private static final Renderer<ConflictingJvmDeclarationsData> CONFLICTING_JVM_DECLARATIONS_DATA = new Renderer<ConflictingJvmDeclarationsData>() {
        @NotNull
        @Override
        public String render(@NotNull ConflictingJvmDeclarationsData data) {
            StringBuilder sb = new StringBuilder();
            for (JvmDeclarationOrigin origin : data.getSignatureOrigins()) {
                DeclarationDescriptor descriptor = origin.getDescriptor();
                if (descriptor != null) {
                    sb.append("    ").append(DescriptorRenderer.COMPACT.render(descriptor)).append("\n");
                }
            }
            return ("The following declarations have the same JVM signature (" + data.getSignature().getName() + data.getSignature().getDesc() + "):\n" + sb).trim();
        }
    };

    public static final DiagnosticFactoryToRendererMap MAP = new DiagnosticFactoryToRendererMap();
    static {
        MAP.put(ErrorsJvm.CONFLICTING_JVM_DECLARATIONS, "Platform declaration clash: {0}", CONFLICTING_JVM_DECLARATIONS_DATA);
        MAP.put(ErrorsJvm.ACCIDENTAL_OVERRIDE, "Accidental override: {0}", CONFLICTING_JVM_DECLARATIONS_DATA);
        MAP.put(ErrorsJvm.PLATFORM_STATIC_NOT_IN_OBJECT, "Only functions in named objects and companion objects of classes can be annotated with 'platformStatic'");
        MAP.put(ErrorsJvm.OVERRIDE_CANNOT_BE_STATIC, "Override member cannot be 'platformStatic' in object");
        MAP.put(ErrorsJvm.OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS, "''jvmOverloads'' annotation has no effect for methods without default arguments");
        MAP.put(ErrorsJvm.OVERLOADS_ABSTRACT, "''jvmOverloads'' annotation cannot be used on abstract methods");
        MAP.put(ErrorsJvm.OVERLOADS_PRIVATE, "''jvmOverloads'' annotation has no effect on private and local declarations");
        MAP.put(ErrorsJvm.NATIVE_DECLARATION_CANNOT_BE_ABSTRACT, "Native declaration can not be abstract");
        MAP.put(ErrorsJvm.NATIVE_DECLARATION_CANNOT_HAVE_BODY, "Native declaration can not have a body");
        MAP.put(ErrorsJvm.NATIVE_DECLARATION_IN_TRAIT, "Members of interfaces can not be native");
        MAP.put(ErrorsJvm.NATIVE_DECLARATION_CANNOT_BE_INLINED, "Members of interfaces can not be inlined");

        MAP.put(ErrorsJvm.POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION, "Only named arguments are available for Java annotations");
        MAP.put(ErrorsJvm.DEPRECATED_ANNOTATION_METHOD_CALL, "Annotation methods are deprecated. Use property instead");
        MAP.put(ErrorsJvm.DEPRECATED_JAVA_ANNOTATION, "This annotation is deprecated in Kotlin. Use ''{0}'' instead", Renderers.TO_STRING);
        MAP.put(ErrorsJvm.NON_SOURCE_REPEATED_ANNOTATION, "Only annotations with SOURCE retention can be repeated on JVM platform");

        MAP.put(ErrorsJvm.NO_REFLECTION_IN_CLASS_PATH, "Call uses reflection API which is not found in compilation classpath. " +
                                                       "Make sure you have kotlin-reflect.jar in the classpath");

        MAP.put(ErrorsJvm.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS,
                "Expected type does not accept nulls in {0}, but the value may be null in {1}", Renderers.TO_STRING, Renderers.TO_STRING);

        MAP.put(ErrorsJvm.TRAIT_CANT_CALL_DEFAULT_METHOD_VIA_SUPER, "Interfaces can't call Java default methods via super");

        MAP.put(ErrorsJvm.WHEN_ENUM_CAN_BE_NULL_IN_JAVA, "Enum argument can be null in Java, but exhaustive when contains no null branch");

        MAP.put(ErrorsJvm.INAPPLICABLE_PUBLIC_FIELD, "publicField annotation is not applicable to this declaration");
    }

    @NotNull
    @Override
    public DiagnosticFactoryToRendererMap getMap() {
        return MAP;
    }
}
