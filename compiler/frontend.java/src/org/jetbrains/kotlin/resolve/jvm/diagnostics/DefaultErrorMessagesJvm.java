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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap;
import org.jetbrains.kotlin.diagnostics.rendering.Renderers;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.renderer.Renderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultErrorMessagesJvm implements DefaultErrorMessages.Extension {

    private static final Renderer<ConflictingJvmDeclarationsData> CONFLICTING_JVM_DECLARATIONS_DATA = new Renderer<ConflictingJvmDeclarationsData>() {
        @NotNull
        @Override
        public String render(@NotNull ConflictingJvmDeclarationsData data) {
            List<String> renderedDescriptors = new ArrayList<String>();
            for (JvmDeclarationOrigin origin : data.getSignatureOrigins()) {
                DeclarationDescriptor descriptor = origin.getDescriptor();
                if (descriptor != null) {
                    renderedDescriptors.add(DescriptorRenderer.COMPACT.render(descriptor));
                }
            }
            Collections.sort(renderedDescriptors);
            StringBuilder sb = new StringBuilder();
            for (String renderedDescriptor : renderedDescriptors) {
                sb.append("    ").append(renderedDescriptor).append("\n");
            }
            return ("The following declarations have the same JVM signature (" + data.getSignature().getName() + data.getSignature().getDesc() + "):\n" + sb).trim();
        }
    };

    public static final DiagnosticFactoryToRendererMap MAP = new DiagnosticFactoryToRendererMap("JVM");
    static {
        MAP.put(ErrorsJvm.CONFLICTING_JVM_DECLARATIONS, "Platform declaration clash: {0}", CONFLICTING_JVM_DECLARATIONS_DATA);
        MAP.put(ErrorsJvm.ACCIDENTAL_OVERRIDE, "Accidental override: {0}", CONFLICTING_JVM_DECLARATIONS_DATA);
        MAP.put(ErrorsJvm.CONFLICTING_INHERITED_JVM_DECLARATIONS, "Inherited platform declarations clash: {0}", CONFLICTING_JVM_DECLARATIONS_DATA);
        MAP.put(ErrorsJvm.JVM_STATIC_NOT_IN_OBJECT, "Only functions in named objects and companion objects of classes can be annotated with ''@JvmStatic''");
        MAP.put(ErrorsJvm.JVM_STATIC_ON_CONST_OR_JVM_FIELD, "''@JvmStatic'' annotation is useless for const or ''@JvmField'' properties");
        MAP.put(ErrorsJvm.OVERRIDE_CANNOT_BE_STATIC, "Override member cannot be ''@JvmStatic'' in object");
        MAP.put(ErrorsJvm.OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS, "''@JvmOverloads'' annotation has no effect for methods without default arguments");
        MAP.put(ErrorsJvm.OVERLOADS_ABSTRACT, "''@JvmOverloads'' annotation cannot be used on abstract methods");
        MAP.put(ErrorsJvm.OVERLOADS_PRIVATE, "''@JvmOverloads'' annotation has no effect on private and local declarations");
        MAP.put(ErrorsJvm.INAPPLICABLE_JVM_NAME, "''@JvmName'' annotation is not applicable to this declaration");
        MAP.put(ErrorsJvm.ILLEGAL_JVM_NAME, "Illegal JVM name");
        MAP.put(ErrorsJvm.VOLATILE_ON_VALUE, "''@Volatile'' annotation cannot be used on immutable properties");
        MAP.put(ErrorsJvm.VOLATILE_ON_DELEGATE, "''@Volatile'' annotation cannot be used on delegated properties");
        MAP.put(ErrorsJvm.SYNCHRONIZED_ON_ABSTRACT, "''@Synchronized'' annotation cannot be used on abstract functions");
        MAP.put(ErrorsJvm.EXTERNAL_DECLARATION_CANNOT_BE_ABSTRACT, "External declaration can not be abstract");
        MAP.put(ErrorsJvm.EXTERNAL_DECLARATION_CANNOT_HAVE_BODY, "External declaration can not have a body");
        MAP.put(ErrorsJvm.EXTERNAL_DECLARATION_IN_INTERFACE, "Members of interfaces can not be external");
        MAP.put(ErrorsJvm.EXTERNAL_DECLARATION_CANNOT_BE_INLINED, "Members of interfaces can not be external");

        MAP.put(ErrorsJvm.POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION, "Only named arguments are available for Java annotations");
        MAP.put(ErrorsJvm.DEPRECATED_JAVA_ANNOTATION, "This annotation is deprecated in Kotlin. Use ''@{0}'' instead", Renderers.TO_STRING);
        MAP.put(ErrorsJvm.NON_SOURCE_REPEATED_ANNOTATION, "Only annotations with SOURCE retention can be repeated on JVM version before 1.8");
        MAP.put(ErrorsJvm.ANNOTATION_IS_NOT_APPLICABLE_TO_MULTIFILE_CLASSES, "Annotation ''@{0}'' is not applicable to the multi-file classes", Renderers.TO_STRING);

        MAP.put(ErrorsJvm.NO_REFLECTION_IN_CLASS_PATH, "Call uses reflection API which is not found in compilation classpath. " +
                                                       "Make sure you have kotlin-reflect.jar in the classpath");

        MAP.put(ErrorsJvm.INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER, "Interfaces can't call Java default methods via super");
        MAP.put(ErrorsJvm.SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC, "Using non-JVM static members protected in the superclass companion is unsupported yet");

        MAP.put(ErrorsJvm.WHEN_ENUM_CAN_BE_NULL_IN_JAVA, "Enum argument can be null in Java, but exhaustive when contains no null branch");

        MAP.put(ErrorsJvm.JAVA_CLASS_ON_COMPANION,
                "The resulting type of this ''javaClass'' call is {0} and not {1}. " +
                "Please use the more clear ''::class.java'' syntax to avoid confusion",
                Renderers.RENDER_TYPE, Renderers.RENDER_TYPE
        );

        MAP.put(ErrorsJvm.JAVA_TYPE_MISMATCH,
                "Java type mismatch expected {1} but found {0}. Use explicit cast", Renderers.RENDER_TYPE, Renderers.RENDER_TYPE);

        MAP.put(ErrorsJvm.DUPLICATE_CLASS_NAMES, "Duplicate JVM class name ''{0}'' generated from: {1}", Renderers.TO_STRING, Renderers.TO_STRING);

        MAP.put(ErrorsJvm.UPPER_BOUND_CANNOT_BE_ARRAY, "Upper bound of a type parameter cannot be an array");

        MAP.put(ErrorsJvm.INAPPLICABLE_JVM_FIELD, "{0}", Renderers.TO_STRING);

        MAP.put(ErrorsJvm.JVM_SYNTHETIC_ON_DELEGATE, "''@JvmSynthetic'' annotation cannot be used on delegated properties");

        MAP.put(ErrorsJvm.SUPER_CALL_WITH_DEFAULT_PARAMETERS, "Super-calls with default arguments are not allowed. Please specify all arguments of ''super.{0}'' explicitly", Renderers.TO_STRING);
    }

    @NotNull
    @Override
    public DiagnosticFactoryToRendererMap getMap() {
        return MAP;
    }
}
