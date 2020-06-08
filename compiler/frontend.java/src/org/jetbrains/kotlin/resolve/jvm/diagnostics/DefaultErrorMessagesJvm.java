/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.diagnostics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.diagnostics.rendering.*;
import org.jetbrains.kotlin.resolve.MemberComparator;
import org.jetbrains.kotlin.utils.StringsKt;

import java.util.List;

import static kotlin.collections.CollectionsKt.*;
import static org.jetbrains.kotlin.diagnostics.rendering.Renderers.*;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm.*;

public class DefaultErrorMessagesJvm implements DefaultErrorMessages.Extension {

    private static final DiagnosticParameterRenderer<ConflictingJvmDeclarationsData> CONFLICTING_JVM_DECLARATIONS_DATA =
            (data, context) -> {
                List<DeclarationDescriptor> renderedDescriptors = sortedWith(
                        mapNotNull(data.getSignatureOrigins(), JvmDeclarationOrigin::getDescriptor),
                        MemberComparator.INSTANCE
                );
                RenderingContext renderingContext = new RenderingContext.Impl(renderedDescriptors);
                return "The following declarations have the same JVM signature " +
                       "(" + data.getSignature().getName() + data.getSignature().getDesc() + "):\n" +
                       StringsKt.join(map(renderedDescriptors, descriptor ->
                               "    " + Renderers.WITHOUT_MODIFIERS.render(descriptor, renderingContext)
                       ), "\n");
            };

    private static final DiagnosticFactoryToRendererMap MAP = new DiagnosticFactoryToRendererMap("JVM");
    static {
        MAP.put(CONFLICTING_JVM_DECLARATIONS, "Platform declaration clash: {0}", CONFLICTING_JVM_DECLARATIONS_DATA);
        MAP.put(ACCIDENTAL_OVERRIDE, "Accidental override: {0}", CONFLICTING_JVM_DECLARATIONS_DATA);
        MAP.put(CONFLICTING_INHERITED_JVM_DECLARATIONS, "Inherited platform declarations clash: {0}", CONFLICTING_JVM_DECLARATIONS_DATA);

        MAP.put(JVM_STATIC_NOT_IN_OBJECT_OR_CLASS_COMPANION, "Only members in named objects and companion objects of classes can be annotated with '@JvmStatic'");
        MAP.put(JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION, "Only members in named objects and companion objects can be annotated with '@JvmStatic'");
        MAP.put(JVM_STATIC_ON_NON_PUBLIC_MEMBER, "Only public members in interface companion objects can be annotated with '@JvmStatic'");
        MAP.put(JVM_STATIC_IN_INTERFACE_1_6, "'@JvmStatic' annotation in interface supported only with JVM target 1.8 and above. Recompile with '-jvm-target 1.8'\"");
        MAP.put(JVM_STATIC_ON_CONST_OR_JVM_FIELD, "'@JvmStatic' annotation is useless for const or '@JvmField' properties");
        MAP.put(OVERRIDE_CANNOT_BE_STATIC, "Override member cannot be '@JvmStatic' in object");
        MAP.put(OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS, "'@JvmOverloads' annotation has no effect for methods without default arguments");
        MAP.put(OVERLOADS_ABSTRACT, "'@JvmOverloads' annotation cannot be used on abstract methods");
        MAP.put(OVERLOADS_INTERFACE, "'@JvmOverloads' annotation cannot be used on interface methods");
        MAP.put(OVERLOADS_PRIVATE, "'@JvmOverloads' annotation has no effect on private declarations");
        MAP.put(OVERLOADS_LOCAL, "'@JvmOverloads' annotation cannot be used on local declarations");
        MAP.put(OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR_WARNING, "'@JvmOverloads' annotation on constructors of annotation classes is deprecated");
        MAP.put(OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR, "'@JvmOverloads' annotation cannot be used on constructors of annotation classes");
        MAP.put(INAPPLICABLE_JVM_NAME, "'@JvmName' annotation is not applicable to this declaration");
        MAP.put(ILLEGAL_JVM_NAME, "Illegal JVM name");
        MAP.put(VOLATILE_ON_VALUE, "'@Volatile' annotation cannot be used on immutable properties");
        MAP.put(VOLATILE_ON_DELEGATE, "'@Volatile' annotation cannot be used on delegated properties");
        MAP.put(SYNCHRONIZED_ON_ABSTRACT, "'@Synchronized' annotation cannot be used on abstract functions");
        MAP.put(SYNCHRONIZED_ON_INLINE, "'@Synchronized' annotation has no effect on inline functions");
        MAP.put(SYNCHRONIZED_IN_INTERFACE, "'@Synchronized' annotation cannot be used on interface members");
        MAP.put(EXTERNAL_DECLARATION_CANNOT_BE_ABSTRACT, "External declaration can not be abstract");
        MAP.put(EXTERNAL_DECLARATION_CANNOT_HAVE_BODY, "External declaration can not have a body");
        MAP.put(EXTERNAL_DECLARATION_IN_INTERFACE, "Members of interfaces can not be external");
        MAP.put(EXTERNAL_DECLARATION_CANNOT_BE_INLINED, "Inline functions can not be external");

        MAP.put(POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION, "Only named arguments are available for Java annotations");
        MAP.put(DEPRECATED_JAVA_ANNOTATION, "This annotation is deprecated in Kotlin. Use ''@{0}'' instead", TO_STRING);
        MAP.put(NON_SOURCE_REPEATED_ANNOTATION, "Repeatable annotations with non-SOURCE retention are not yet supported");
        MAP.put(ANNOTATION_IS_NOT_APPLICABLE_TO_MULTIFILE_CLASSES, "Annotation ''@{0}'' is not applicable to the multi-file classes", TO_STRING);

        MAP.put(JVM_PACKAGE_NAME_CANNOT_BE_EMPTY, "''@JvmPackageName'' annotation value cannot be empty");
        MAP.put(JVM_PACKAGE_NAME_MUST_BE_VALID_NAME, "''@JvmPackageName'' annotation value must be a valid dot-qualified name of a package");
        MAP.put(JVM_PACKAGE_NAME_NOT_SUPPORTED_IN_FILES_WITH_CLASSES, "''@JvmPackageName'' annotation is not supported for files with class declarations");

        MAP.put(STATE_IN_MULTIFILE_CLASS, "Non-const property with backing field or delegate is not allowed in a multi-file class if -Xmultifile-parts-inherit is enabled");

        MAP.put(NO_REFLECTION_IN_CLASS_PATH, "Call uses reflection API which is not found in compilation classpath. " +
                                             "Make sure you have kotlin-reflect.jar in the classpath");

        MAP.put(INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER, "Interfaces can call default methods via super only within @JvmDefault members. Please annotate the containing interface member with @JvmDefault");
        MAP.put(SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC, "Using non-JVM static members protected in the superclass companion is unsupported yet");

        MAP.put(NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS, "Type mismatch: inferred type is {1} but {0} was expected", RENDER_TYPE, RENDER_TYPE);
        MAP.put(RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS,
                "Unsafe use of a nullable receiver of type {0}", RENDER_TYPE);

        MAP.put(ANNOTATION_TARGETS_NON_EXISTENT_ACCESSOR,
                "An accessor will not be generated for ''{0}'', so the annotation will not be written to the class file", STRING);

        MAP.put(WHEN_ENUM_CAN_BE_NULL_IN_JAVA, "Enum argument can be null in Java, but exhaustive when contains no null branch");

        MAP.put(JAVA_CLASS_ON_COMPANION,
                "The resulting type of this ''javaClass'' call is {0} and not {1}. " +
                "Please use the more clear ''::class.java'' syntax to avoid confusion",
                RENDER_TYPE, RENDER_TYPE
        );

        MAP.put(JAVA_TYPE_MISMATCH,
                "Java type mismatch expected {1} but found {0}. Use explicit cast", RENDER_TYPE, RENDER_TYPE);

        MAP.put(DUPLICATE_CLASS_NAMES, "Duplicate JVM class name ''{0}'' generated from: {1}", STRING, STRING);

        MAP.put(UPPER_BOUND_CANNOT_BE_ARRAY, "Upper bound of a type parameter cannot be an array");

        MAP.put(INAPPLICABLE_JVM_FIELD, "{0}", STRING);

        MAP.put(JVM_SYNTHETIC_ON_DELEGATE, "'@JvmSynthetic' annotation cannot be used on delegated properties");

        MAP.put(STRICTFP_ON_CLASS, "'@Strictfp' annotation on classes is unsupported yet");

        MAP.put(SUPER_CALL_WITH_DEFAULT_PARAMETERS, "Super-calls with default arguments are not allowed. Please specify all arguments of ''super.{0}'' explicitly", STRING);

        MAP.put(TARGET6_INTERFACE_INHERITANCE,
                "Compiling ''{0}'' to JVM 1.8, but its superinterface ''{1}'' was compiled for JVM 1.6. " +
                "Method implementation inheritance is restricted for such cases. " +
                "Please make explicit overrides (abstract or concrete) for the following non-abstract members of ''{1}'': {2}",
                NAME, NAME, STRING);

        MAP.put(DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET, "Super calls to Java default methods are deprecated in JVM target 1.6. Recompile with '-jvm-target 1.8'");
        MAP.put(DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET_ERROR, "Super calls to Java default methods are prohibited in JVM target 1.6. Recompile with '-jvm-target 1.8'");
        MAP.put(INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET, "Calls to static methods in Java interfaces are deprecated in JVM target 1.6. Recompile with '-jvm-target 1.8'");
        MAP.put(INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET_ERROR, "Calls to static methods in Java interfaces are prohibited in JVM target 1.6. Recompile with '-jvm-target 1.8'");

        MAP.put(INLINE_FROM_HIGHER_PLATFORM, "Cannot inline bytecode built with {0} into bytecode that is being built with {1}. Please specify proper ''-jvm-target'' option", STRING, STRING);
        MAP.put(INLINE_FROM_HIGHER_PLATFORM_WARNING, "Cannot inline bytecode built with {0} into bytecode that is being built with {1}. Please specify proper ''-jvm-target'' option", STRING, STRING);

        MAP.put(JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE, "Symbol is declared in module ''{0}'' which current module does not depend on", STRING);
        MAP.put(JAVA_MODULE_DOES_NOT_READ_UNNAMED_MODULE, "Symbol is declared in unnamed module which is not read by current module");
        MAP.put(JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE, "Symbol is declared in module ''{0}'' which does not export package ''{1}''", STRING, STRING);

        MAP.put(API_VERSION_IS_AT_LEAST_ARGUMENT_SHOULD_BE_CONSTANT, "'apiVersionIsAtLeast' argument should be a constant expression");

        MAP.put(ASSIGNMENT_TO_ARRAY_LOOP_VARIABLE, "Assignment to a for-in-array loop range variable. Behavior may change in Kotlin 1.3. " +
                                                   "See https://youtrack.jetbrains.com/issue/KT-21354 for more details");

        MAP.put(JVM_DEFAULT_NOT_IN_INTERFACE,"'@JvmDefault' is only supported on interface members");
        MAP.put(JVM_DEFAULT_IN_JVM6_TARGET,"''@{0}'' is only supported since JVM target 1.8. Recompile with ''-jvm-target 1.8''", STRING);
        MAP.put(JVM_DEFAULT_REQUIRED_FOR_OVERRIDE, "'@JvmDefault' is required for an override of a '@JvmDefault' member");
        MAP.put(JVM_DEFAULT_IN_DECLARATION, "Usage of ''@{0}'' is only allowed with -Xjvm-default option", STRING);
        MAP.put(JVM_DEFAULT_THROUGH_INHERITANCE, "Inheritance from an interface with '@JvmDefault' members is only allowed with -Xjvm-default option");
        MAP.put(USAGE_OF_JVM_DEFAULT_THROUGH_SUPER_CALL, "Super calls of '@JvmDefault' members are only allowed with -Xjvm-default option");
        MAP.put(NON_JVM_DEFAULT_OVERRIDES_JAVA_DEFAULT, "Non-@JvmDefault interface method cannot override default Java method. Please annotate this method with @JvmDefault");
        MAP.put(EXPLICIT_METADATA_IS_DISALLOWED, "Explicit @Metadata is disallowed");
        MAP.put(SUSPENSION_POINT_INSIDE_MONITOR, "A suspension point at {0} is inside a critical section", STRING);
        MAP.put(SUSPENSION_POINT_INSIDE_CRITICAL_SECTION, "The ''{0}'' suspension point is inside a critical section", NAME);

        String MESSAGE_FOR_CONCURRENT_HASH_MAP_CONTAINS =
                "Method 'contains' from ConcurrentHashMap may have unexpected semantics: it calls 'containsValue' instead of 'containsKey'. " +
                "Use explicit form of the call to 'containsKey'/'containsValue'/'contains' or cast the value to kotlin.collections.Map instead. " +
                "See https://youtrack.jetbrains.com/issue/KT-18053 for more details";
        MAP.put(CONCURRENT_HASH_MAP_CONTAINS_OPERATOR, MESSAGE_FOR_CONCURRENT_HASH_MAP_CONTAINS);
        MAP.put(CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR, MESSAGE_FOR_CONCURRENT_HASH_MAP_CONTAINS);
        MAP.put(TAILREC_WITH_DEFAULTS, "Computation order of non-constant default arguments in tail-recursive functions will change in 1.4. See KT-31540 for more details");

        String spreadOnPolymorphicSignature = "Spread operator is prohibited for arguments to signature-polymorphic calls";
        MAP.put(SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL, spreadOnPolymorphicSignature);
        MAP.put(SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL_ERROR, spreadOnPolymorphicSignature);

        MAP.put(FUNCTION_DELEGATE_MEMBER_NAME_CLASH,
                "Functional interface member cannot have this name on JVM because it clashes with an internal member getFunctionDelegate");
    }

    @NotNull
    @Override
    public DiagnosticFactoryToRendererMap getMap() {
        return MAP;
    }
}
