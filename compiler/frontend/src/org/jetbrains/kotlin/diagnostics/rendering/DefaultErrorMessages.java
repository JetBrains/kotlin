/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.diagnostics.rendering;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.io.FileUtil;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.config.LanguageVersion;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.resolve.VarianceConflictDiagnosticData;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.SinceKotlinInfo;
import org.jetbrains.kotlin.types.KotlinTypeKt;
import org.jetbrains.kotlin.util.MappedExtensionProvider;
import org.jetbrains.kotlin.util.OperatorNameConventions;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.diagnostics.rendering.Renderers.*;
import static org.jetbrains.kotlin.diagnostics.rendering.RenderingContext.of;

public class DefaultErrorMessages {

    public interface Extension {
        ExtensionPointName<Extension> EP_NAME = ExtensionPointName.create("org.jetbrains.kotlin.defaultErrorMessages");

        @NotNull
        DiagnosticFactoryToRendererMap getMap();
    }

    private static final DiagnosticFactoryToRendererMap MAP = new DiagnosticFactoryToRendererMap("Default");
    private static final MappedExtensionProvider<Extension, List<DiagnosticFactoryToRendererMap>> RENDERER_MAPS = MappedExtensionProvider.create(
            Extension.EP_NAME,
            extensions -> {
                List<DiagnosticFactoryToRendererMap> result = new ArrayList<>(extensions.size() + 1);
                for (Extension extension : extensions) {
                    result.add(extension.getMap());
                }
                result.add(MAP);
                return result;
            });

    @NotNull
    public static String render(@NotNull Diagnostic diagnostic) {
        for (DiagnosticFactoryToRendererMap map : RENDERER_MAPS.get()) {
            DiagnosticRenderer renderer = map.get(diagnostic.getFactory());
            if (renderer != null) {
                //noinspection unchecked
                return renderer.render(diagnostic);
            }
        }
        throw new IllegalArgumentException("Don't know how to render diagnostic of type " + diagnostic.getFactory().getName() +
                                           " with the following renderer maps: " + RENDERER_MAPS.get());
    }

    @TestOnly
    @Nullable
    public static DiagnosticRenderer getRendererForDiagnostic(@NotNull Diagnostic diagnostic) {
        for (DiagnosticFactoryToRendererMap map : RENDERER_MAPS.get()) {
            DiagnosticRenderer renderer = map.get(diagnostic.getFactory());

            if (renderer != null) return renderer;
        }

        return null;
    }

    static {
        MAP.put(UNRESOLVED_REFERENCE, "Unresolved reference: {0}", ELEMENT_TEXT);

        MAP.put(INVISIBLE_REFERENCE, "Cannot access ''{0}'': it is {1} in {2}", NAME, VISIBILITY, NAME_OF_CONTAINING_DECLARATION_OR_FILE);
        MAP.put(INVISIBLE_MEMBER, "Cannot access ''{0}'': it is {1} in {2}", NAME, VISIBILITY, NAME_OF_CONTAINING_DECLARATION_OR_FILE);

        MAP.put(PROTECTED_CONSTRUCTOR_NOT_IN_SUPER_CALL, "Protected constructor ''{0}'' from other classes can only be used in super-call", Renderers.SHORT_NAMES_IN_TYPES);

        MAP.put(EXPOSED_PROPERTY_TYPE, "''{0}'' property exposes its ''{2}'' type{1}", TO_STRING, TO_STRING, TO_STRING);
        MAP.put(EXPOSED_FUNCTION_RETURN_TYPE, "''{0}'' function exposes its ''{2}'' return type{1}", TO_STRING, TO_STRING, TO_STRING);
        MAP.put(EXPOSED_PARAMETER_TYPE, "''{0}'' function exposes its ''{2}'' parameter type{1}", TO_STRING, TO_STRING, TO_STRING);
        MAP.put(EXPOSED_RECEIVER_TYPE, "''{0}'' member exposes its ''{2}'' receiver type{1}", TO_STRING, TO_STRING, TO_STRING);
        MAP.put(EXPOSED_TYPE_PARAMETER_BOUND, "''{0}'' generic exposes its ''{2}'' parameter bound type{1}", TO_STRING, TO_STRING, TO_STRING);
        MAP.put(EXPOSED_SUPER_CLASS, "''{0}'' subclass exposes its ''{2}'' supertype{1}", TO_STRING, TO_STRING, TO_STRING);
        MAP.put(EXPOSED_SUPER_INTERFACE, "''{0}'' sub-interface exposes its ''{2}'' supertype{1}", TO_STRING, TO_STRING, TO_STRING);
        MAP.put(EXPOSED_TYPEALIAS_EXPANDED_TYPE, "''{0}'' typealias exposes ''{2}'' in expanded type{1}", TO_STRING, TO_STRING, TO_STRING);

        MAP.put(EXTENSION_SHADOWED_BY_MEMBER, "Extension is shadowed by a member: {0}", COMPACT_WITH_MODIFIERS);
        MAP.put(EXTENSION_FUNCTION_SHADOWED_BY_INNER_CLASS_CONSTRUCTOR,
                "Extension function is shadowed by an inner class constructor: {0}", COMPACT_WITH_MODIFIERS);
        MAP.put(EXTENSION_FUNCTION_SHADOWED_BY_MEMBER_PROPERTY_WITH_INVOKE,
                "Extension function is shadowed by a member property ''{0}'' with {1}", NAME, COMPACT_WITH_MODIFIERS);

        MAP.put(INACCESSIBLE_TYPE, "Type {0} is inaccessible in this context due to: {1}", RENDER_TYPE, commaSeparated(FQ_NAMES_IN_TYPES));

        MAP.put(REDECLARATION, "Conflicting declarations: {0}", commaSeparated(COMPACT_WITH_MODIFIERS));
        MAP.put(PACKAGE_OR_CLASSIFIER_REDECLARATION, "Redeclaration: {0}", STRING);
        MAP.put(DUPLICATE_PARAMETER_NAME_IN_FUNCTION_TYPE, "Duplicate parameter name in function type");

        MAP.put(NAME_SHADOWING, "Name shadowed: {0}", STRING);
        MAP.put(ACCESSOR_PARAMETER_NAME_SHADOWING, "Accessor parameter name 'field' is shadowed by backing field variable");

        MAP.put(TYPE_MISMATCH, "Type mismatch: inferred type is {1} but {0} was expected", RENDER_TYPE, RENDER_TYPE);
        MAP.put(TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN,
                "Inferred type is a function type, but a non-function type {0} was expected. Use either ''= ...'' or '''{ ... }'', but not both.",
                RENDER_TYPE);
        MAP.put(TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS,
                "Type mismatch: inferred type is {1} but {0} was expected. Projected type {2} restricts use of {3}",
                object -> {
                    RenderingContext context =
                            of(object.getExpectedType(), object.getExpressionType(), object.getReceiverType(), object.getCallableDescriptor());
                    return new String[] {
                            RENDER_TYPE.render(object.getExpectedType(), context),
                            RENDER_TYPE.render(object.getExpressionType(), context),
                            RENDER_TYPE.render(object.getReceiverType(), context),
                            FQ_NAMES_IN_TYPES.render(object.getCallableDescriptor(), context)
                    };
                });

        MAP.put(MEMBER_PROJECTED_OUT, "Out-projected type ''{1}'' prohibits the use of ''{0}''", FQ_NAMES_IN_TYPES, RENDER_TYPE);
        MAP.put(INCOMPATIBLE_MODIFIERS, "Modifier ''{0}'' is incompatible with ''{1}''", TO_STRING, TO_STRING);
        MAP.put(DEPRECATED_MODIFIER_PAIR, "Modifier ''{0}'' is deprecated in presence of ''{1}''", TO_STRING, TO_STRING);
        MAP.put(REPEATED_MODIFIER, "Repeated ''{0}''", TO_STRING);
        MAP.put(WRONG_MODIFIER_TARGET, "Modifier ''{0}'' is not applicable to ''{1}''", TO_STRING, TO_STRING);
        MAP.put(DEPRECATED_MODIFIER_FOR_TARGET, "Modifier ''{0}'' is deprecated for ''{1}''", TO_STRING, TO_STRING);
        MAP.put(REDUNDANT_MODIFIER_FOR_TARGET, "Modifier ''{0}'' is redundant for ''{1}''", TO_STRING, TO_STRING);
        MAP.put(WRONG_MODIFIER_CONTAINING_DECLARATION, "Modifier ''{0}'' is not applicable inside ''{1}''", TO_STRING, TO_STRING);
        MAP.put(DEPRECATED_MODIFIER_CONTAINING_DECLARATION, "Modifier ''{0}'' is deprecated inside ''{1}''", TO_STRING, TO_STRING);
        MAP.put(ILLEGAL_INLINE_PARAMETER_MODIFIER, "Modifier ''{0}'' is allowed only for function parameters of an inline function", TO_STRING);
        MAP.put(INLINE_SUSPEND_FUNCTION_TYPE_UNSUPPORTED, "Inline lambda parameters of suspend function type are not fully supported. Add 'noinline' modifier.");
        MAP.put(WRONG_ANNOTATION_TARGET, "This annotation is not applicable to target ''{0}''", TO_STRING);
        MAP.put(WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET, "This annotation is not applicable to target ''{0}'' and use site target ''@{1}''", TO_STRING, TO_STRING);
        MAP.put(WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET_ON_TYPE,
                "Use of this annotation with target ''type'' and use site target ''@{0}'' is deprecated", TO_STRING);
        MAP.put(REPEATED_ANNOTATION, "This annotation is not repeatable");
        MAP.put(NON_SOURCE_ANNOTATION_ON_INLINED_LAMBDA_EXPRESSION, "The lambda expression here is an inlined argument so this annotation cannot be stored anywhere");

        MAP.put(INAPPLICABLE_TARGET_ON_PROPERTY, "''@{0}:'' annotations could be applied only to property declarations", TO_STRING);
        MAP.put(INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE, "''@{0}:'' annotations could be applied only to mutable properties", TO_STRING);
        MAP.put(INAPPLICABLE_TARGET_PROPERTY_HAS_NO_DELEGATE, "'@delegate:' annotations could be applied only to delegated properties");
        MAP.put(INAPPLICABLE_TARGET_PROPERTY_HAS_NO_BACKING_FIELD, "'@field:' annotations could be applied only to properties with backing fields");
        MAP.put(INAPPLICABLE_PARAM_TARGET, "'@param:' annotations could be applied only to primary constructor parameters");
        MAP.put(REDUNDANT_ANNOTATION_TARGET, "Redundant annotation target ''{0}''", STRING);
        MAP.put(INAPPLICABLE_FILE_TARGET, "'@file:' annotations can only be applied before package declaration");

        MAP.put(ILLEGAL_SINCE_KOTLIN_VALUE, "Invalid @SinceKotlin annotation value (should be 'major.minor' or 'major.minor.patch')");
        MAP.put(NEWER_VERSION_IN_SINCE_KOTLIN, "The version is greater than the specified API version {0}", STRING);

        MAP.put(REDUNDANT_MODIFIER, "Modifier ''{0}'' is redundant because ''{1}'' is present", TO_STRING, TO_STRING);
        MAP.put(REDUNDANT_OPEN_IN_INTERFACE, "Modifier 'open' is redundant for abstract interface members");
        MAP.put(REDUNDANT_MODIFIER_IN_GETTER, "Visibility modifiers are redundant in getter");
        MAP.put(TYPE_PARAMETERS_IN_ENUM, "Enum class cannot have type parameters");
        MAP.put(TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM,
                "Type checking has run into a recursive problem. Easiest workaround: specify types of your declarations explicitly"); // TODO: message
        MAP.put(RETURN_NOT_ALLOWED, "'return' is not allowed here");
        MAP.put(PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE, "Projections are not allowed for immediate arguments of a supertype");
        MAP.put(LABEL_NAME_CLASH, "There is more than one label with such a name in this scope");
        MAP.put(EXPRESSION_EXPECTED_PACKAGE_FOUND, "Expression expected, but a package name found");

        MAP.put(CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON, "Cannot import-on-demand from object ''{0}''", NAME);
        MAP.put(CANNOT_BE_IMPORTED, "Cannot import ''{0}'', functions and properties can be imported only from packages or objects", TO_STRING);
        MAP.put(PACKAGE_CANNOT_BE_IMPORTED, "Packages cannot be imported");
        MAP.put(CONFLICTING_IMPORT, "Conflicting import, imported name ''{0}'' is ambiguous", STRING);
        MAP.put(OPERATOR_RENAMED_ON_IMPORT, "Operator renamed to a different operator on import");
        MAP.put(PLATFORM_CLASS_MAPPED_TO_KOTLIN, "This class shouldn''t be used in Kotlin. Use {0} instead.", CLASSES_OR_SEPARATED);

        MAP.put(CANNOT_INFER_PARAMETER_TYPE, "Cannot infer a type for this parameter. Please specify it explicitly.");

        MAP.put(MIXING_NAMED_AND_POSITIONED_ARGUMENTS, "Mixing named and positioned arguments is not allowed");
        MAP.put(ARGUMENT_PASSED_TWICE, "An argument is already passed for this parameter");
        MAP.put(NAMED_PARAMETER_NOT_FOUND, "Cannot find a parameter with this name: {0}", ELEMENT_TEXT);
        MAP.put(NAMED_ARGUMENTS_NOT_ALLOWED, "Named arguments are not allowed for {0}", (target, context) -> {
            switch (target) {
                case NON_KOTLIN_FUNCTION:
                    return "non-Kotlin functions";
                case INVOKE_ON_FUNCTION_TYPE:
                    return "function types";
                case EXPECTED_CLASS_MEMBER:
                    return "members of header classes";
                default:
                    throw new AssertionError(target);
            }
        });

        MAP.put(VARARG_OUTSIDE_PARENTHESES, "Passing value as a vararg is only allowed inside a parenthesized argument list");
        MAP.put(NON_VARARG_SPREAD, "The spread operator (*foo) may only be applied in a vararg position");
        MAP.put(SPREAD_OF_NULLABLE, "The spread operator (*foo) may not be applied to an argument of nullable type");
        MAP.put(SPREAD_OF_LAMBDA_OR_CALLABLE_REFERENCE, "The spread operator (*foo) cannot be applied to lambda argument or callable reference");

        MAP.put(MANY_LAMBDA_EXPRESSION_ARGUMENTS, "Only one lambda expression is allowed outside a parenthesized argument list");
        MAP.put(PROPERTY_WITH_NO_TYPE_NO_INITIALIZER, "This property must either have a type annotation, be initialized or be delegated");
        MAP.put(VARIABLE_WITH_NO_TYPE_NO_INITIALIZER, "This variable must either have a type annotation or be initialized");

        MAP.put(INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION, "Initializer required for destructuring declaration");
        MAP.put(COMPONENT_FUNCTION_MISSING, "Destructuring declaration initializer of type {1} must have a ''{0}()'' function", TO_STRING, RENDER_TYPE);
        MAP.put(COMPONENT_FUNCTION_ON_NULLABLE, "Not nullable value required to call ''{0}()'' function of destructuring declaration initializer", TO_STRING);
        MAP.put(COMPONENT_FUNCTION_AMBIGUITY, "Function ''{0}()'' is ambiguous for this expression: {1}", TO_STRING, AMBIGUOUS_CALLS);
        MAP.put(COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH, "''{0}()'' function returns ''{1}'', but ''{2}'' is expected",
                                                                                    TO_STRING, RENDER_TYPE, RENDER_TYPE);

        MAP.put(ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS, "This property cannot be declared abstract");
        MAP.put(ABSTRACT_PROPERTY_WITH_INITIALIZER, "Property with initializer cannot be abstract");
        MAP.put(ABSTRACT_PROPERTY_WITH_GETTER, "Property with getter implementation cannot be abstract");
        MAP.put(ABSTRACT_PROPERTY_WITH_SETTER, "Property with setter implementation cannot be abstract");

        MAP.put(ABSTRACT_DELEGATED_PROPERTY, "Delegated property cannot be abstract");
        MAP.put(ACCESSOR_FOR_DELEGATED_PROPERTY, "Delegated property cannot have accessors with non-default implementations");
        MAP.put(DELEGATED_PROPERTY_IN_INTERFACE, "Delegated properties are not allowed in interfaces");

        MAP.put(INAPPLICABLE_LATEINIT_MODIFIER, "''lateinit'' modifier {0}", STRING);
        MAP.put(LATEINIT_INTRINSIC_CALL_ON_NON_LITERAL, "This declaration can only be called on a property literal (e.g. 'Foo::bar')");
        MAP.put(LATEINIT_INTRINSIC_CALL_ON_NON_LATEINIT, "This declaration can only be called on a reference to a lateinit property");
        MAP.put(LATEINIT_INTRINSIC_CALL_IN_INLINE_FUNCTION, "This declaration can not be used inside an inline function");
        MAP.put(LATEINIT_INTRINSIC_CALL_ON_NON_ACCESSIBLE_PROPERTY, "Backing field of ''{0}'' is not accessible at this point", COMPACT);

        MAP.put(GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY, "Getter visibility must be the same as property visibility");
        MAP.put(SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY, "Setter visibility must be the same or less permissive than property visibility");
        MAP.put(PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY, "Private setters are not allowed for abstract properties");
        MAP.put(PRIVATE_SETTER_FOR_OPEN_PROPERTY, "Private setters are not allowed for open properties");
        MAP.put(BACKING_FIELD_IN_INTERFACE, "Property in an interface cannot have a backing field");
        MAP.put(MUST_BE_INITIALIZED, "Property must be initialized");
        MAP.put(MUST_BE_INITIALIZED_OR_BE_ABSTRACT, "Property must be initialized or be abstract");
        MAP.put(EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT, "Extension property must have accessors or be abstract");
        MAP.put(UNNECESSARY_LATEINIT, "Lateinit is unnecessary: definitely initialized in constructors");
        MAP.put(PROPERTY_INITIALIZER_IN_INTERFACE, "Property initializers are not allowed in interfaces");
        MAP.put(PRIVATE_PROPERTY_IN_INTERFACE, "Abstract property in an interface cannot be private");
        MAP.put(EXTENSION_PROPERTY_WITH_BACKING_FIELD, "Extension property cannot be initialized because it has no backing field");
        MAP.put(PROPERTY_INITIALIZER_NO_BACKING_FIELD, "Initializer is not allowed here because this property has no backing field");
        MAP.put(ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS, "Abstract property ''{0}'' in non-abstract class ''{1}''", STRING, NAME);
        MAP.put(ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS, "Abstract function ''{0}'' in non-abstract class ''{1}''", STRING, NAME);
        MAP.put(ABSTRACT_FUNCTION_WITH_BODY, "A function ''{0}'' with body cannot be abstract", NAME);
        MAP.put(NON_ABSTRACT_FUNCTION_WITH_NO_BODY, "Function ''{0}'' without a body must be abstract", NAME);
        MAP.put(PRIVATE_FUNCTION_WITH_NO_BODY, "Function ''{0}'' without body cannot be private", NAME);

        MAP.put(NON_MEMBER_FUNCTION_NO_BODY, "Function ''{0}'' must have a body", NAME);
        MAP.put(FUNCTION_DECLARATION_WITH_NO_NAME, "Function declaration must have a name");
        MAP.put(ANONYMOUS_FUNCTION_WITH_NAME, "Anonymous functions with names are prohibited");
        MAP.put(NON_FINAL_MEMBER_IN_FINAL_CLASS, "'open' has no effect in a final class");
        MAP.put(NON_FINAL_MEMBER_IN_OBJECT, "'open' has no effect in an object");

        MAP.put(ANONYMOUS_FUNCTION_PARAMETER_WITH_DEFAULT_VALUE, "An anonymous function is not allowed to specify default values for its parameters");
        MAP.put(USELESS_VARARG_ON_PARAMETER, "Vararg on this parameter is useless");
        MAP.put(MULTIPLE_VARARG_PARAMETERS, "Multiple vararg-parameters are prohibited");
        MAP.put(FORBIDDEN_VARARG_PARAMETER_TYPE, "Forbidden vararg parameter type: {0}", RENDER_TYPE);

        MAP.put(EXPECTED_DECLARATION_WITH_BODY, "Header declaration must not have a body");
        MAP.put(EXPECTED_DECLARATION_WITH_DEFAULT_PARAMETER, "Header declaration cannot have parameters with default values");
        MAP.put(EXPECTED_CLASS_CONSTRUCTOR_DELEGATION_CALL, "Explicit delegation call for constructor of a header class is not allowed");
        MAP.put(EXPECTED_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER, "Header class constructor cannot have a property parameter");
        MAP.put(EXPECTED_ENUM_CONSTRUCTOR, "Header enum class cannot have a constructor");
        MAP.put(EXPECTED_ENUM_ENTRY_WITH_BODY, "Header enum entry cannot have a body");
        MAP.put(EXPECTED_PROPERTY_INITIALIZER, "Header property cannot have an initializer");

        MAP.put(ACTUAL_TYPE_ALIAS_NOT_TO_CLASS, "Right-hand side of 'impl' type alias should be a class, not another type alias");
        MAP.put(ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE, "Aliased class should not have type parameters with declaration-site variance");
        MAP.put(ACTUAL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE, "Right-hand side of 'impl' type alias cannot contain use-site variance or star projections");
        MAP.put(ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION, "Type arguments in the right-hand side of 'impl' type alias should be its type parameters in the same order, e.g. 'impl typealias Foo<A, B> = Bar<A, B>'");

        MAP.put(NO_ACTUAL_FOR_EXPECT, "''header'' {0} has no implementation in module{1}{2}", DECLARATION_NAME_WITH_KIND,
                PLATFORM, PlatformIncompatibilityDiagnosticRenderer.TEXT);
        MAP.put(ACTUAL_WITHOUT_EXPECT, "''impl'' {0} has no corresponding ''header'' declaration{1}", DECLARATION_NAME_WITH_KIND,
                PlatformIncompatibilityDiagnosticRenderer.TEXT);

        MAP.put(NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS, "''impl'' class ''{0}'' has no implementation of ''header'' class members:{1}",
                NAME, IncompatibleHeaderImplClassScopesRenderer.TEXT);
        MAP.put(ACTUAL_MISSING, "Declaration should be marked with 'actual' (suppress with -Xno-check-actual)");

        MAP.put(PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT, "Projections are not allowed on type arguments of functions and properties");
        MAP.put(SUPERTYPE_NOT_INITIALIZED, "This type has a constructor, and thus must be initialized here");
        MAP.put(NOTHING_TO_OVERRIDE, "''{0}'' overrides nothing", NAME);
        MAP.put(VIRTUAL_MEMBER_HIDDEN, "''{0}'' hides member of supertype ''{2}'' and needs ''override'' modifier", NAME, NAME, NAME);

        MAP.put(DATA_CLASS_OVERRIDE_CONFLICT, "Function ''{0}'' generated for the data class conflicts with member of supertype ''{1}''", NAME, NAME);
        MAP.put(DATA_CLASS_OVERRIDE_DEFAULT_VALUES_WARNING, "Function ''{0}'' generated for the data class has default values for parameters, and conflicts with member of supertype ''{1}''", NAME, NAME);
        MAP.put(DATA_CLASS_OVERRIDE_DEFAULT_VALUES_ERROR, "Function ''{0}'' generated for the data class has default values for parameters, and conflicts with member of supertype ''{1}''", NAME, NAME);

        MAP.put(CANNOT_OVERRIDE_INVISIBLE_MEMBER, "''{0}'' has no access to ''{1}'', so it cannot override it", FQ_NAMES_IN_TYPES,
                FQ_NAMES_IN_TYPES);
        MAP.put(CANNOT_INFER_VISIBILITY, "Cannot infer visibility for ''{0}''. Please specify it explicitly", COMPACT);

        MAP.put(ENUM_ENTRY_SHOULD_BE_INITIALIZED, "Enum has no default constructor, use 'entry(parameters)'");

        MAP.put(UNINITIALIZED_VARIABLE, "Variable ''{0}'' must be initialized", NAME);
        MAP.put(UNINITIALIZED_PARAMETER, "Parameter ''{0}'' is uninitialized here", NAME);
        MAP.put(UNINITIALIZED_ENUM_ENTRY, "Enum entry ''{0}'' is uninitialized here", NAME);
        MAP.put(UNINITIALIZED_ENUM_COMPANION, "Companion object of enum class ''{0}'' is uninitialized here", NAME);
        MAP.put(UNUSED_VARIABLE, "Variable ''{0}'' is never used", NAME);
        MAP.put(UNUSED_PARAMETER, "Parameter ''{0}'' is never used", NAME);
        MAP.put(UNUSED_ANONYMOUS_PARAMETER, "Parameter ''{0}'' is never used, could be renamed to _", NAME);
        MAP.put(UNUSED_DESTRUCTURED_PARAMETER_ENTRY, "Destructured parameter ''{0}'' is never used", NAME);
        MAP.put(ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE, "Variable ''{0}'' is assigned but never accessed", NAME);
        MAP.put(VARIABLE_WITH_REDUNDANT_INITIALIZER, "Variable ''{0}'' initializer is redundant", NAME);
        MAP.put(UNUSED_VALUE, "The value ''{0}'' assigned to ''{1}'' is never used", ELEMENT_TEXT, FQ_NAMES_IN_TYPES);
        MAP.put(UNUSED_CHANGED_VALUE, "The value changed at ''{0}'' is never used", ELEMENT_TEXT);
        MAP.put(UNUSED_EXPRESSION, "The expression is unused");
        MAP.put(UNUSED_LAMBDA_EXPRESSION, "The lambda expression is unused. If you mean a block, you can use 'run { ... }'");

        MAP.put(VAL_REASSIGNMENT, "Val cannot be reassigned", NAME);
        MAP.put(VAL_REASSIGNMENT_VIA_BACKING_FIELD, "Reassignment of read-only property via backing field is deprecated", NAME);
        MAP.put(VAL_REASSIGNMENT_VIA_BACKING_FIELD_ERROR, "Reassignment of read-only property via backing field", NAME);
        MAP.put(CAPTURED_VAL_INITIALIZATION, "Captured values initialization is forbidden due to possible reassignment", NAME);
        MAP.put(CAPTURED_MEMBER_VAL_INITIALIZATION, "Captured member values initialization is forbidden due to possible reassignment", NAME);
        MAP.put(SETTER_PROJECTED_OUT, "Setter for ''{0}'' is removed by type projection", NAME);
        MAP.put(INVISIBLE_SETTER, "Cannot assign to ''{0}'': the setter is {1} in {2}", NAME, VISIBILITY,
                NAME_OF_CONTAINING_DECLARATION_OR_FILE);
        MAP.put(INITIALIZATION_BEFORE_DECLARATION, "Variable cannot be initialized before declaration", NAME);
        MAP.put(VARIABLE_EXPECTED, "Variable expected");

        MAP.put(VAL_OR_VAR_ON_LOOP_PARAMETER, "''{0}'' on loop parameter is not allowed", TO_STRING);
        MAP.put(VAL_OR_VAR_ON_FUN_PARAMETER, "''{0}'' on function parameter is not allowed", TO_STRING);
        MAP.put(VAL_OR_VAR_ON_CATCH_PARAMETER, "''{0}'' on catch parameter is not allowed", TO_STRING);
        MAP.put(VAL_OR_VAR_ON_SECONDARY_CONSTRUCTOR_PARAMETER, "''{0}'' on secondary constructor parameter is not allowed", TO_STRING);

        MAP.put(UNREACHABLE_CODE, "Unreachable code", TO_STRING);

        MAP.put(MANY_COMPANION_OBJECTS, "Only one companion object is allowed per class");

        MAP.put(DEPRECATION, "''{0}'' is deprecated. {1}", DEPRECATION_RENDERER, STRING);
        MAP.put(DEPRECATION_ERROR, "Using ''{0}'' is an error. {1}", DEPRECATION_RENDERER, STRING);

        MAP.put(TYPEALIAS_EXPANSION_DEPRECATION, "''{0}'' uses ''{1}'', which is deprecated. {2}", DEPRECATION_RENDERER, DEPRECATION_RENDERER, STRING);
        MAP.put(TYPEALIAS_EXPANSION_DEPRECATION_ERROR, "''{0}'' uses ''{1}'', which is an error. {2}", DEPRECATION_RENDERER, DEPRECATION_RENDERER, STRING);

        DiagnosticParameterRenderer<Pair<LanguageVersion, String>> sinceKotlinInfoMessage = (pair, renderingContext) -> {
            String message = pair.getSecond();
            return pair.getFirst().getVersionString() + (message != null ? ". " + message : "");
        };
        MAP.put(SINCE_KOTLIN_INFO_DEPRECATION, "''{0}''{1} should not be used in Kotlin {2}", DEPRECATION_RENDERER,
                (obj, renderingContext) -> obj.equals(SinceKotlinInfo.Version.INFINITY) ? "" : " is only supported since Kotlin " + obj.asString() + " and",
                sinceKotlinInfoMessage);
        MAP.put(SINCE_KOTLIN_INFO_DEPRECATION_ERROR, "''{0}''{1} cannot be used in Kotlin {2}", DEPRECATION_RENDERER,
                (obj, renderingContext) -> obj.equals(SinceKotlinInfo.Version.INFINITY) ? "" : " is only available since Kotlin " + obj.asString() + " and",
                sinceKotlinInfoMessage);

        MAP.put(API_NOT_AVAILABLE, "This declaration is only available since Kotlin {0} and cannot be used with the specified API version {1}", STRING, STRING);

        MAP.put(MISSING_DEPENDENCY_CLASS, "Cannot access class ''{0}''. Check your module classpath for missing or conflicting dependencies", TO_STRING);
        MAP.put(PRE_RELEASE_CLASS, "{0} is compiled by a pre-release version of Kotlin and cannot be loaded by this version of the compiler", TO_STRING);
        MAP.put(INCOMPATIBLE_CLASS,
                "{0} was compiled with an incompatible version of Kotlin. {1}",
                TO_STRING,
                (incompatibility, renderingContext) ->
                        "The binary version of its metadata is " + incompatibility.getActualVersion() +
                        ", expected version is " + incompatibility.getExpectedVersion() + ".\n" +
                        "The class is loaded from " + FileUtil.toSystemIndependentName(incompatibility.getFilePath())
        );

        MAP.put(LOCAL_OBJECT_NOT_ALLOWED, "Named object ''{0}'' is a singleton and cannot be local. Try to use anonymous object instead", NAME);
        MAP.put(LOCAL_INTERFACE_NOT_ALLOWED, "''{0}'' is an interface so it cannot be local. Try to use anonymous object or abstract class instead", NAME);
        MAP.put(TYPE_PARAMETERS_IN_OBJECT, "Type parameters are not allowed for objects");
        MAP.put(ENUM_CLASS_CONSTRUCTOR_CALL, "Enum types cannot be instantiated");
        MAP.put(SEALED_CLASS_CONSTRUCTOR_CALL, "Sealed types cannot be instantiated");

        MAP.put(DELEGATION_IN_INTERFACE, "Interfaces cannot use delegation");
        MAP.put(DELEGATION_NOT_TO_INTERFACE, "Only interfaces can be delegated to");
        MAP.put(DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE, "Delegated member ''{0}'' hides supertype override: {1}. Please specify proper override explicitly", COMPACT, commaSeparated(SHORT_NAMES_IN_TYPES));
        MAP.put(NO_CONSTRUCTOR, "This class does not have a constructor");
        MAP.put(RESOLUTION_TO_CLASSIFIER, "{2}", NAME, TO_STRING, STRING);
        MAP.put(NOT_A_CLASS, "Not a class");
        MAP.put(ILLEGAL_ESCAPE_SEQUENCE, "Illegal escape sequence");

        MAP.put(RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS, "Left-hand side of callable reference matches expression syntax reserved for future releases");

        MAP.put(LOCAL_EXTENSION_PROPERTY, "Local extension properties are not allowed");
        MAP.put(LOCAL_VARIABLE_WITH_GETTER, "Local variables are not allowed to have getters");
        MAP.put(LOCAL_VARIABLE_WITH_SETTER, "Local variables are not allowed to have setters");
        MAP.put(VAL_WITH_SETTER, "A 'val'-property cannot have a setter");

        MAP.put(DEPRECATED_IDENTITY_EQUALS, "Identity equality for arguments of types {0} and {1} is deprecated", RENDER_TYPE, RENDER_TYPE);
        MAP.put(IMPLICIT_BOXING_IN_IDENTITY_EQUALS, "Identity equality for arguments of types {0} and {1} can be unstable because of implicit boxing", RENDER_TYPE, RENDER_TYPE);

        MAP.put(DEPRECATED_BINARY_MOD, "Deprecated convention for ''{0}''. Use ''{1}''", NAME, STRING);
        MAP.put(DEPRECATED_BINARY_MOD_AS_REM, "''%'' is resolved to deprecated ''{0}'' operator. Replace with ''.{0}'' or add operator ''{1}''", NAME, STRING);

        MAP.put(NO_GET_METHOD, "No get method providing array access");
        MAP.put(NO_SET_METHOD, "No set method providing array access");

        MAP.put(INC_DEC_SHOULD_NOT_RETURN_UNIT, "Functions inc(), dec() shouldn't return Unit to be used by operators ++, --");
        MAP.put(ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT, "Function ''{0}'' should return Unit to be used by corresponding operator ''{1}''",
                NAME, ELEMENT_TEXT);
        MAP.put(ASSIGN_OPERATOR_AMBIGUITY, "Assignment operators ambiguity: {0}", AMBIGUOUS_CALLS);

        MAP.put(EQUALS_MISSING, "No method 'equals(Any?): Boolean' available");
        MAP.put(ASSIGNMENT_IN_EXPRESSION_CONTEXT, "Assignments are not expressions, and only expressions are allowed in this context");
        MAP.put(SUPER_IS_NOT_AN_EXPRESSION, "''{0}'' is not an expression, it can only be used on the left-hand side of a dot (''.'')", STRING);
        MAP.put(SUPER_CANT_BE_EXTENSION_RECEIVER, "''{0}'' is not an expression, it can not be used as a receiver for extension functions", STRING);
        MAP.put(DECLARATION_IN_ILLEGAL_CONTEXT, "Declarations are not allowed in this position");
        MAP.put(SETTER_PARAMETER_WITH_DEFAULT_VALUE, "Setter parameters cannot have default values");
        MAP.put(NO_THIS, "'this' is not defined in this context");
        MAP.put(SUPER_NOT_AVAILABLE, "No supertypes are accessible in this context");
        MAP.put(SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE, "Superclass is not accessible from interface");
        MAP.put(AMBIGUOUS_SUPER, "Many supertypes available, please specify the one you mean in angle brackets, e.g. 'super<Foo>'");
        MAP.put(ABSTRACT_SUPER_CALL, "Abstract member cannot be accessed directly");
        MAP.put(NOT_A_SUPERTYPE, "Not an immediate supertype");
        MAP.put(TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER, "Type arguments do not need to be specified in a 'super' qualifier");
        MAP.put(QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE, "Explicitly qualified supertype is extended by another supertype ''{0}''", RENDER_TYPE);
        MAP.put(USELESS_CAST, "No cast needed");
        MAP.put(CAST_NEVER_SUCCEEDS, "This cast can never succeed");
        MAP.put(DYNAMIC_NOT_ALLOWED, "Dynamic types are not allowed in this position");
        MAP.put(IS_ENUM_ENTRY, "'is' over enum entry is not allowed, use comparison instead");
        MAP.put(ENUM_ENTRY_AS_TYPE, "Use of enum entry names as types is not allowed, use enum type instead");
        MAP.put(USELESS_NULLABLE_CHECK, "Non-null type is checked for instance of nullable type");
        MAP.put(USELESS_IS_CHECK, "Check for instance is always ''{0}''", TO_STRING);
        MAP.put(WRONG_SETTER_PARAMETER_TYPE, "Setter parameter type must be equal to the type of the property, i.e. ''{0}''", RENDER_TYPE, RENDER_TYPE);
        MAP.put(WRONG_GETTER_RETURN_TYPE, "Getter return type must be equal to the type of the property, i.e. ''{0}''", RENDER_TYPE, RENDER_TYPE);
        MAP.put(WRONG_SETTER_RETURN_TYPE, "Setter return type must be Unit");
        MAP.put(NO_COMPANION_OBJECT, "Classifier ''{0}'' does not have a companion object, and thus must be initialized here", NAME);
        MAP.put(TYPE_PARAMETER_IS_NOT_AN_EXPRESSION, "Type parameter ''{0}'' is not an expression", NAME);
        MAP.put(TYPE_PARAMETER_ON_LHS_OF_DOT, "Type parameter ''{0}'' cannot have or inherit a companion object, so it cannot be on the left hand side of dot", NAME);
        MAP.put(NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE, "Nested {0} accessed via instance reference", RENDER_CLASS_OR_OBJECT_NAME);
        MAP.put(NESTED_CLASS_SHOULD_BE_QUALIFIED, "Nested {0} should be qualified as ''{1}''", RENDER_CLASS_OR_OBJECT_NAME, TO_STRING);

        MAP.put(INACCESSIBLE_OUTER_CLASS_EXPRESSION, "Expression is inaccessible from a nested class ''{0}''", NAME);
        MAP.put(NESTED_CLASS_NOT_ALLOWED, "{0} is not allowed here", STRING);
        MAP.put(NESTED_CLASS_DEPRECATED, "{0} is deprecated here", STRING);

        MAP.put(HAS_NEXT_MISSING, "hasNext() cannot be called on iterator() of type ''{0}''", RENDER_TYPE);
        MAP.put(HAS_NEXT_FUNCTION_AMBIGUITY, "hasNext() is ambiguous for iterator() of type ''{0}''", RENDER_TYPE);
        MAP.put(HAS_NEXT_FUNCTION_NONE_APPLICABLE, "None of the hasNext() functions is applicable for iterator() of type ''{0}''", RENDER_TYPE);
        MAP.put(HAS_NEXT_FUNCTION_TYPE_MISMATCH, "The ''iterator().hasNext()'' function of the loop range must return Boolean, but returns {0}", RENDER_TYPE);

        MAP.put(NEXT_MISSING, "''next()'' cannot be called on ''iterator()'' of type ''{0}''", RENDER_TYPE);
        MAP.put(NEXT_AMBIGUITY, "''next()'' is ambiguous for ''iterator()'' of type ''{0}''", RENDER_TYPE);
        MAP.put(NEXT_NONE_APPLICABLE, "None of the ''next()'' functions is applicable for ''iterator()'' of type ''{0}''", RENDER_TYPE);

        MAP.put(ITERATOR_MISSING, "For-loop range must have an 'iterator()' method");
        MAP.put(ITERATOR_ON_NULLABLE, "Not nullable value required to call an 'iterator()' method on for-loop range");
        MAP.put(ITERATOR_AMBIGUITY, "Method ''iterator()'' is ambiguous for this expression: {0}", AMBIGUOUS_CALLS);

        MAP.put(DELEGATE_SPECIAL_FUNCTION_MISSING, "Missing ''{0}'' method on delegate of type ''{1}''", STRING, RENDER_TYPE);
        MAP.put(DELEGATE_SPECIAL_FUNCTION_AMBIGUITY, "Overload resolution ambiguity on method ''{0}'': {1}",  STRING, AMBIGUOUS_CALLS);
        MAP.put(DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE, "Property delegate must have a ''{0}'' method. None of the following functions is suitable: {1}",
                STRING, AMBIGUOUS_CALLS);
        MAP.put(DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH, "The ''{0}'' function of property delegate is expected to return ''{1}'', but returns ''{2}''",
                STRING, RENDER_TYPE, RENDER_TYPE);
        MAP.put(DELEGATE_PD_METHOD_NONE_APPLICABLE, "''{0}'' method may be missing. None of the following functions will be called: {1}", STRING, AMBIGUOUS_CALLS);

        MAP.put(COMPARE_TO_TYPE_MISMATCH, "''compareTo()'' must return Int, but returns {0}", RENDER_TYPE);

        MAP.put(UNDERSCORE_IS_RESERVED, "Names _, __, ___, ..., are reserved in Kotlin");
        MAP.put(UNDERSCORE_USAGE_WITHOUT_BACKTICKS, "Names _, __, ___, ... can be used only in back-ticks (`_`, `__`, `___`, ...)");
        MAP.put(YIELD_IS_RESERVED, "{0}", STRING);
        MAP.put(INVALID_CHARACTERS, "Name {0}", STRING);

        MAP.put(INAPPLICABLE_OPERATOR_MODIFIER, "''operator'' modifier is inapplicable on this function: {0}", STRING);
        MAP.put(INAPPLICABLE_INFIX_MODIFIER, "''infix'' modifier is inapplicable on this function: {0}", STRING);

        MAP.put(OPERATOR_MODIFIER_REQUIRED, "''operator'' modifier is required on ''{0}'' in ''{1}''", NAME, STRING);
        MAP.put(INFIX_MODIFIER_REQUIRED, "''infix'' modifier is required on ''{0}'' in ''{1}''", NAME, STRING);

        MAP.put(INAPPLICABLE_MODIFIER, "''{0}'' modifier is inapplicable. The reason is that {1}", TO_STRING, STRING);

        MAP.put(DSL_SCOPE_VIOLATION, "''{0}'' can''t be called in this context by implicit receiver. " +
                                     "Use the explicit one if necessary", COMPACT);

        MAP.put(RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY,
                "Returns are not allowed for functions with expression body. Use block body in '{...}'");
        MAP.put(NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY, "A 'return' expression required in a function with a block body ('{...}')");
        MAP.put(RETURN_TYPE_MISMATCH, "This function must return a value of type {0}", RENDER_TYPE);
        MAP.put(EXPECTED_TYPE_MISMATCH, "Expected a value of type {0}", RENDER_TYPE);
        MAP.put(ASSIGNMENT_TYPE_MISMATCH,
                "Expected a value of type {0}. Assignment operation is not an expression, so it does not return any value", RENDER_TYPE);

        MAP.put(EXPECTED_PARAMETER_TYPE_MISMATCH, "Expected parameter of type {0}", RENDER_TYPE);
        MAP.put(EXPECTED_PARAMETERS_NUMBER_MISMATCH, "Expected {0,choice,0#no parameters|1#one parameter of type|1<{0,number,integer} parameters of types} {1}", null, RENDER_COLLECTION_OF_TYPES);

        MAP.put(IMPLICIT_CAST_TO_ANY, "Conditional branch result of type {0} is implicitly cast to {1}",
                RENDER_TYPE, RENDER_TYPE);
        MAP.put(EXPRESSION_EXPECTED, "{0} is not an expression, and only expressions are allowed here", (expression, context) -> {
            String expressionType = expression.toString();
            return expressionType.substring(0, 1) +
                   expressionType.substring(1).toLowerCase();
        });

        MAP.put(UPPER_BOUND_VIOLATED, "Type argument is not within its bounds: should be subtype of ''{0}''", RENDER_TYPE, RENDER_TYPE);
        MAP.put(FINAL_UPPER_BOUND, "''{0}'' is a final type, and thus a value of the type parameter is predetermined", RENDER_TYPE);
        MAP.put(UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE, "Extension function type can not be used as an upper bound");
        MAP.put(ONLY_ONE_CLASS_BOUND_ALLOWED, "Only one of the upper bounds can be a class");
        MAP.put(BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER, "Type parameter cannot have any other bounds if it's bounded by another type parameter");
        MAP.put(REPEATED_BOUND, "Type parameter already has this bound");
        MAP.put(DYNAMIC_UPPER_BOUND, "Dynamic type can not be used as an upper bound");
        MAP.put(USELESS_ELVIS, "Elvis operator (?:) always returns the left operand of non-nullable type {0}", RENDER_TYPE);
        MAP.put(USELESS_ELVIS_ON_LAMBDA_EXPRESSION, "Left operand of elvis operator (?:) is a lambda expression");
        MAP.put(USELESS_ELVIS_ON_CALLABLE_REFERENCE, "Left operand of elvis operator (?:) is a callable reference expression");
        MAP.put(USELESS_ELVIS_RIGHT_IS_NULL, "Right operand of elvis operator (?:) is useless if it is null");
        MAP.put(CONFLICTING_UPPER_BOUNDS, "Upper bounds of {0} have empty intersection", NAME);

        MAP.put(TOPLEVEL_TYPEALIASES_ONLY, "Nested and local type aliases are not supported");
        MAP.put(RECURSIVE_TYPEALIAS_EXPANSION, "Recursive type alias in expansion: {0}", NAME);
        MAP.put(UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION,
                "Type argument resulting from type alias expansion is not within required bounds for ''{2}'': " +
                "should be subtype of ''{0}'', substituted type is ''{1}''",
                RENDER_TYPE, RENDER_TYPE, NAME);
        MAP.put(CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION, "Conflicting projection in type alias expansion in intermediate type ''{0}''", RENDER_TYPE);
        MAP.put(TYPEALIAS_SHOULD_EXPAND_TO_CLASS, "Type alias expands to {0}, which is not a class, an interface, or an object", RENDER_TYPE);
        MAP.put(TYPEALIAS_EXPANDED_TO_MALFORMED_TYPE, "Type alias expanded to malformed type {0}: {1}", RENDER_TYPE, STRING);
        MAP.put(UNUSED_TYPEALIAS_PARAMETER, "Type alias parameter {0} is not used in the expanded type {1} and does not affect type checking", NAME, RENDER_TYPE);
        MAP.put(EXPANDED_TYPE_CANNOT_BE_CONSTRUCTED, "Expanded type {0} contains non-invariant projections in top-level arguments and cannot be constructed", RENDER_TYPE);
        MAP.put(EXPANDED_TYPE_CANNOT_BE_INHERITED, "Expanded type {0} contains non-invariant projections in top-level arguments and cannot be inherited from", RENDER_TYPE);

        MAP.put(MODIFIER_LIST_NOT_ALLOWED, "Modifiers and annotations are not allowed here, because there are other modifiers or annotations outside of parenthesis");

        MAP.put(TOO_MANY_ARGUMENTS, "Too many arguments for {0}", FQ_NAMES_IN_TYPES);

        MAP.put(CONSTANT_EXPECTED_TYPE_MISMATCH, "The {0} literal does not conform to the expected type {1}", STRING, RENDER_TYPE);
        MAP.put(DIVISION_BY_ZERO, "Division by zero");
        MAP.put(INTEGER_OVERFLOW, "This operation has led to an overflow");
        MAP.put(INT_LITERAL_OUT_OF_RANGE, "The value is out of range");
        MAP.put(WRONG_LONG_SUFFIX, "Use 'L' instead of 'l'");
        MAP.put(FLOAT_LITERAL_OUT_OF_RANGE, "The value is out of range");
        MAP.put(FLOAT_LITERAL_CONFORMS_INFINITY, "Floating point number conforms to infinity");
        MAP.put(FLOAT_LITERAL_CONFORMS_ZERO, "Floating point number conforms to zero");
        MAP.put(INCORRECT_CHARACTER_LITERAL, "Incorrect character literal");
        MAP.put(EMPTY_CHARACTER_LITERAL, "Empty character literal");
        MAP.put(ILLEGAL_UNDERSCORE, "Illegal underscore");
        MAP.put(TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL, "Too many characters in a character literal ''{0}''", ELEMENT_TEXT);
        MAP.put(ILLEGAL_ESCAPE, "Illegal escape: ''{0}''", ELEMENT_TEXT);
        MAP.put(NULL_FOR_NONNULL_TYPE, "Null can not be a value of a non-null type {0}", RENDER_TYPE);

        MAP.put(ELSE_MISPLACED_IN_WHEN, "'else' entry must be the last one in a when-expression");
        MAP.put(REDUNDANT_ELSE_IN_WHEN, "'when' is exhaustive so 'else' is redundant here");
        MAP.put(COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT, "Deprecated syntax. Use '||' instead of commas in when-condition for 'when' without argument");
        MAP.put(DUPLICATE_LABEL_IN_WHEN, "Duplicate label in when");

        MAP.put(NO_ELSE_IN_WHEN, "''when'' expression must be exhaustive, add necessary {0}", RENDER_WHEN_MISSING_CASES);
        MAP.put(NON_EXHAUSTIVE_WHEN, "''when'' expression on enum is recommended to be exhaustive, add {0}", RENDER_WHEN_MISSING_CASES);
        MAP.put(NON_EXHAUSTIVE_WHEN_ON_SEALED_CLASS, "''when'' expression on sealed classes is recommended to be exhaustive, add {0}", RENDER_WHEN_MISSING_CASES);

        MAP.put(TYPE_MISMATCH_IN_RANGE, "Type mismatch: incompatible types of range and element checked in it");
        MAP.put(CYCLIC_INHERITANCE_HIERARCHY, "There's a cycle in the inheritance hierarchy for this type");
        MAP.put(CYCLIC_GENERIC_UPPER_BOUND, "Type parameter has cyclic upper bounds");

        MAP.put(MANY_CLASSES_IN_SUPERTYPE_LIST, "Only one class may appear in a supertype list");
        MAP.put(SUPERTYPE_NOT_A_CLASS_OR_INTERFACE, "Only classes and interfaces may serve as supertypes");
        MAP.put(SUPERTYPE_IS_EXTENSION_FUNCTION_TYPE, "Extension function type is not allowed as supertypes");
        MAP.put(SUPERTYPE_IS_SUSPEND_FUNCTION_TYPE, "Suspend function type is not allowed as supertypes");
        MAP.put(SUPERTYPE_INITIALIZED_IN_INTERFACE, "Interfaces cannot initialize supertypes");
        MAP.put(CLASS_IN_SUPERTYPE_FOR_ENUM, "Enum class cannot inherit from classes");
        MAP.put(CONSTRUCTOR_IN_INTERFACE, "An interface may not have a constructor");
        MAP.put(METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE, "An interface may not implement a method of 'Any'");
        MAP.put(INTERFACE_WITH_SUPERCLASS, "An interface cannot inherit from a class");
        MAP.put(SUPERTYPE_APPEARS_TWICE, "A supertype appears twice");
        MAP.put(FINAL_SUPERTYPE, "This type is final, so it cannot be inherited from");
        MAP.put(DATA_CLASS_CANNOT_HAVE_CLASS_SUPERTYPES, "Data class inheritance from other classes is forbidden");
        MAP.put(SEALED_SUPERTYPE, "This type is sealed, so it can be inherited by only its own nested classes or objects");
        MAP.put(SEALED_SUPERTYPE_IN_LOCAL_CLASS, "Local class cannot extend a sealed class");
        MAP.put(SINGLETON_IN_SUPERTYPE, "Cannot inherit from a singleton");
        MAP.put(CLASS_CANNOT_BE_EXTENDED_DIRECTLY, "Class {0} cannot be extended directly", NAME);

        MAP.put(CYCLIC_CONSTRUCTOR_DELEGATION_CALL, "There's a cycle in the delegation calls chain");
        MAP.put(CONSTRUCTOR_IN_OBJECT, "Constructors are not allowed for objects");
        MAP.put(SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR, "Supertype initialization is impossible without primary constructor");
        MAP.put(PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED, "Primary constructor call expected");
        MAP.put(DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR, "Call to super is not allowed in enum constructor");
        MAP.put(PRIMARY_CONSTRUCTOR_REQUIRED_FOR_DATA_CLASS, "Primary constructor required for data class");
        MAP.put(EXPLICIT_DELEGATION_CALL_REQUIRED,
                "Explicit 'this' or 'super' call is required. There is no constructor in superclass that can be called without arguments");

        MAP.put(INSTANCE_ACCESS_BEFORE_SUPER_CALL, "Cannot access ''{0}'' before superclass constructor has been called", NAME);

        MAP.put(ILLEGAL_SELECTOR, "The expression cannot be a selector (occur after a dot)");

        MAP.put(NO_TAIL_CALLS_FOUND, "A function is marked as tail-recursive but no tail calls are found");
        MAP.put(VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION, "A type annotation is required on a value parameter");
        MAP.put(BREAK_OR_CONTINUE_OUTSIDE_A_LOOP, "'break' and 'continue' are only allowed inside a loop");
        MAP.put(BREAK_OR_CONTINUE_IN_WHEN, "'break' and 'continue' are not allowed in 'when' statements. Consider using labels to continue/break from the outer loop");
        MAP.put(BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY, "'break' or 'continue' jumps across a function or a class boundary");
        MAP.put(NOT_A_LOOP_LABEL, "The label ''{0}'' does not denote a loop", STRING);

        MAP.put(ANONYMOUS_INITIALIZER_IN_INTERFACE, "Anonymous initializers are not allowed in interfaces");
        MAP.put(NULLABLE_SUPERTYPE, "A supertype cannot be nullable");
        MAP.put(DYNAMIC_SUPERTYPE, "A supertype cannot be dynamic");
        MAP.put(REDUNDANT_NULLABLE, "Redundant '?'");
        MAP.put(UNSAFE_CALL, "Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type {0}", RENDER_TYPE);
        MAP.put(UNSAFE_IMPLICIT_INVOKE_CALL, "Reference has a nullable type ''{0}'', use explicit ''?.invoke()'' to make a function-like call instead", RENDER_TYPE);
        MAP.put(AMBIGUOUS_LABEL, "Ambiguous label");
        MAP.put(UNSUPPORTED, "Unsupported [{0}]", STRING);
        MAP.put(NEW_INFERENCE_ERROR, "New inference error [{0}]", STRING);
        MAP.put(NEW_INFERENCE_DIAGNOSTIC, "New inference [{0}]", STRING);

        MAP.put(UNSUPPORTED_FEATURE, "{0}", new LanguageFeatureMessageRenderer(LanguageFeatureMessageRenderer.Type.UNSUPPORTED));
        MAP.put(EXPERIMENTAL_FEATURE_WARNING, "{0}", new LanguageFeatureMessageRenderer(LanguageFeatureMessageRenderer.Type.WARNING));
        MAP.put(EXPERIMENTAL_FEATURE_ERROR, "{0}", new LanguageFeatureMessageRenderer(LanguageFeatureMessageRenderer.Type.ERROR));

        MAP.put(EXCEPTION_FROM_ANALYZER, "Internal Error occurred while analyzing this expression:\n{0}", THROWABLE);
        MAP.put(MISSING_STDLIB, "{0}. Ensure you have the standard Kotlin library in dependencies", STRING);
        MAP.put(UNNECESSARY_SAFE_CALL, "Unnecessary safe call on a non-null receiver of type {0}", RENDER_TYPE);
        MAP.put(UNEXPECTED_SAFE_CALL, "Safe-call is not allowed here");
        MAP.put(UNNECESSARY_NOT_NULL_ASSERTION, "Unnecessary non-null assertion (!!) on a non-null receiver of type {0}", RENDER_TYPE);
        MAP.put(NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION, "Non-null assertion (!!) is called on a lambda expression");
        MAP.put(NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE, "Non-null assertion (!!) is called on a callable reference expression");
        MAP.put(NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER, "{0} does not refer to a type parameter of {1}", (typeConstraint, context) -> {
            //noinspection ConstantConditions
            return typeConstraint.getSubjectTypeParameterName().getReferencedName();
        }, DECLARATION_NAME);
        MAP.put(SMARTCAST_IMPOSSIBLE,
                "Smart cast to ''{0}'' is impossible, because ''{1}'' is a {2}", RENDER_TYPE, STRING, STRING);
        MAP.put(ALWAYS_NULL, "The result of the expression is always null");

        MAP.put(MISSING_CONSTRUCTOR_KEYWORD, "Use 'constructor' keyword after modifiers of primary constructor");
        MAP.put(MISSING_CONSTRUCTOR_BRACKETS, "Constructor requires brackets");

        MAP.put(NON_PRIVATE_CONSTRUCTOR_IN_ENUM, "Constructor must be private in enum class");
        MAP.put(NON_PRIVATE_CONSTRUCTOR_IN_SEALED, "Constructor must be private in sealed class");

        MAP.put(VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED, "Variance annotations are only allowed for type parameters of classes and interfaces");
        MAP.put(BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED, "Bounds are not allowed on type alias parameters");

        MAP.put(DEPRECATED_TYPE_PARAMETER_SYNTAX, "Type parameters must be placed before the name of the function");

        MAP.put(MISPLACED_TYPE_PARAMETER_CONSTRAINTS, "If a type parameter has multiple constraints, they all need to be placed in the 'where' clause");

        MultiRenderer<VarianceConflictDiagnosticData> varianceConflictDataRenderer = data -> {
            RenderingContext context =
                    of(data.getTypeParameter(), data.getTypeParameter().getVariance(), data.getOccurrencePosition(),
                       data.getContainingType());
            return new String[] {
                    NAME.render(data.getTypeParameter(), context),
                    RENDER_POSITION_VARIANCE.render(data.getTypeParameter().getVariance(), context),
                    RENDER_POSITION_VARIANCE.render(data.getOccurrencePosition(), context),
                    RENDER_TYPE.render(data.getContainingType(), context)
            };
        };
        MAP.put(TYPE_VARIANCE_CONFLICT, "Type parameter {0} is declared as ''{1}'' but occurs in ''{2}'' position in type {3}",
                varianceConflictDataRenderer);
        MAP.put(TYPE_VARIANCE_CONFLICT_IN_EXPANDED_TYPE, "Type parameter {0} is declared as ''{1}'' but occurs in ''{2}'' position in abbreviated type {3}",
                varianceConflictDataRenderer);

        MAP.put(FINITE_BOUNDS_VIOLATION, "This type parameter violates the Finite Bound Restriction");
        MAP.put(FINITE_BOUNDS_VIOLATION_IN_JAVA, "Violation of Finite Bound Restriction for {0}", STRING);
        MAP.put(EXPANSIVE_INHERITANCE, "This type parameter violates the Non-Expansive Inheritance Restriction");
        MAP.put(EXPANSIVE_INHERITANCE_IN_JAVA, "Violation of Non-Expansive Inheritance Restriction for {0}", STRING);
        MAP.put(REDUNDANT_PROJECTION, "Projection is redundant: the corresponding type parameter of {0} has the same variance", NAME);
        MAP.put(CONFLICTING_PROJECTION, "Projection is conflicting with variance of the corresponding type parameter of {0}. Remove the projection or replace it with ''*''", NAME);

        MAP.put(TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED, "Type arguments for outer class are redundant when nested class is referenced");

        MAP.put(REIFIED_TYPE_IN_CATCH_CLAUSE, "Reified type is forbidden for catch parameter");
        MAP.put(TYPE_PARAMETER_IN_CATCH_CLAUSE, "Type parameter is forbidden for catch parameter");
        MAP.put(GENERIC_THROWABLE_SUBCLASS, "Subclass of 'Throwable' may not have type parameters");

        MAP.put(TYPE_MISMATCH_IN_FOR_LOOP, "The loop iterates over values of type {0} but the parameter is declared to be {1}", RENDER_TYPE,
                RENDER_TYPE);
        MAP.put(INCOMPATIBLE_TYPES, "Incompatible types: {0} and {1}", RENDER_TYPE, RENDER_TYPE);
        MAP.put(IMPLICIT_NOTHING_RETURN_TYPE, "'Nothing' return type needs to be specified explicitly");
        MAP.put(IMPLICIT_NOTHING_PROPERTY_TYPE, "'Nothing' property type needs to be specified explicitly");
        MAP.put(ABBREVIATED_NOTHING_RETURN_TYPE, "'Nothing' return type can't be specified with type alias");
        MAP.put(ABBREVIATED_NOTHING_PROPERTY_TYPE, "'Nothing' property type can't be specified with type alias");
        MAP.put(IMPLICIT_INTERSECTION_TYPE, "Inferred type {0} is an intersection, please specify the required type explicitly", RENDER_TYPE);
        MAP.put(EXPECTED_CONDITION, "Expected condition of type Boolean");

        MAP.put(DYNAMIC_RECEIVER_NOT_ALLOWED, "Dynamic receiver is prohibited");

        MAP.put(CANNOT_CHECK_FOR_ERASED, "Cannot check for instance of erased type: {0}", RENDER_TYPE);
        MAP.put(UNCHECKED_CAST, "Unchecked cast: {0} to {1}", RENDER_TYPE, RENDER_TYPE);

        MAP.put(INCONSISTENT_TYPE_PARAMETER_VALUES, "Type parameter {0} of ''{1}'' has inconsistent values: {2}", NAME, NAME, RENDER_COLLECTION_OF_TYPES);
        MAP.put(INCONSISTENT_TYPE_PARAMETER_BOUNDS, "Type parameter {0} of ''{1}'' has inconsistent bounds: {2}", NAME, NAME, RENDER_COLLECTION_OF_TYPES);

        MAP.put(EQUALITY_NOT_APPLICABLE, "Operator ''{0}'' cannot be applied to ''{1}'' and ''{2}''", (nameExpression, context) -> {
            //noinspection ConstantConditions
            return nameExpression.getReferencedName();
        }, RENDER_TYPE, RENDER_TYPE);

        MAP.put(SENSELESS_COMPARISON, "Condition ''{0}'' is always ''{1}''", ELEMENT_TEXT, TO_STRING);
        MAP.put(SENSELESS_NULL_IN_WHEN, "Expression under 'when' is never equal to null");

        MAP.put(INVALID_IF_AS_EXPRESSION, "'if' must have both main and 'else' branches if used as an expression");

        MAP.put(OVERRIDING_FINAL_MEMBER, "''{0}'' in ''{1}'' is final and cannot be overridden", NAME, NAME);
        MAP.put(CANNOT_WEAKEN_ACCESS_PRIVILEGE, "Cannot weaken access privilege ''{0}'' for ''{1}'' in ''{2}''", VISIBILITY, NAME, NAME);
        MAP.put(CANNOT_CHANGE_ACCESS_PRIVILEGE, "Cannot change access privilege ''{0}'' for ''{1}'' in ''{2}''", VISIBILITY, NAME, NAME);

        MAP.put(RETURN_TYPE_MISMATCH_ON_OVERRIDE, "Return type of ''{0}'' is not a subtype of the return type of the overridden member ''{1}''",
                NAME, FQ_NAMES_IN_TYPES);
        MAP.put(RETURN_TYPE_MISMATCH_ON_INHERITANCE, "''{0}'' clashes with ''{1}'': return types are incompatible",
                SHORT_NAMES_IN_TYPES, SHORT_NAMES_IN_TYPES);

        MAP.put(PROPERTY_TYPE_MISMATCH_ON_OVERRIDE, "Type of ''{0}'' is not a subtype of the overridden property ''{1}''",
                NAME, FQ_NAMES_IN_TYPES);
        MAP.put(VAR_TYPE_MISMATCH_ON_OVERRIDE, "Type of ''{0}'' doesn''t match the type of the overridden var-property ''{1}''",
                NAME, FQ_NAMES_IN_TYPES);
        MAP.put(PROPERTY_TYPE_MISMATCH_ON_INHERITANCE, "''{0}'' clashes with ''{1}'': property types are incompatible",
                SHORT_NAMES_IN_TYPES, SHORT_NAMES_IN_TYPES);
        MAP.put(VAR_TYPE_MISMATCH_ON_INHERITANCE, "''{0}'' clashes with ''{1}'': property types do not match",
                SHORT_NAMES_IN_TYPES, SHORT_NAMES_IN_TYPES);

        MAP.put(OVERRIDING_FINAL_MEMBER_BY_DELEGATION, "''{0}'' implicitly overrides a final member ''{1}'' by delegation",
                SHORT_NAMES_IN_TYPES, SHORT_NAMES_IN_TYPES);
        MAP.put(VAR_OVERRIDDEN_BY_VAL_BY_DELEGATION, "Val-property ''{0}'' implicitly overrides a var-property ''{1}'' by delegation",
                SHORT_NAMES_IN_TYPES, SHORT_NAMES_IN_TYPES);
        MAP.put(RETURN_TYPE_MISMATCH_BY_DELEGATION, "Type of ''{0}'' is not a subtype of overridden by delegation ''{1}''",
                SHORT_NAMES_IN_TYPES, SHORT_NAMES_IN_TYPES);
        MAP.put(PROPERTY_TYPE_MISMATCH_BY_DELEGATION, "Type of property ''{0}'' is not a subtype of overridden by delegation ''{1}''",
                SHORT_NAMES_IN_TYPES, SHORT_NAMES_IN_TYPES);

        MAP.put(VAR_OVERRIDDEN_BY_VAL, "Var-property {0} cannot be overridden by val-property {1}", FQ_NAMES_IN_TYPES, FQ_NAMES_IN_TYPES);

        MAP.put(CONFLICTING_INHERITED_MEMBERS, "{0} inherits conflicting members: {1}", NAME, commaSeparated(FQ_NAMES_IN_TYPES));
        MAP.put(ABSTRACT_MEMBER_NOT_IMPLEMENTED, "{0} is not abstract and does not implement abstract member {1}", RENDER_CLASS_OR_OBJECT,
                FQ_NAMES_IN_TYPES);
        MAP.put(ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED, "{0} is not abstract and does not implement abstract base class member {1}",
                RENDER_CLASS_OR_OBJECT, FQ_NAMES_IN_TYPES);

        MAP.put(MANY_IMPL_MEMBER_NOT_IMPLEMENTED, "{0} must override {1} because it inherits many implementations of it",
                RENDER_CLASS_OR_OBJECT, FQ_NAMES_IN_TYPES);
        MAP.put(MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED, "{0} must override {1} because it inherits multiple interface methods of it",
                RENDER_CLASS_OR_OBJECT, FQ_NAMES_IN_TYPES);

        MAP.put(CONFLICTING_OVERLOADS, "Conflicting overloads: {0}", commaSeparated(FQ_NAMES_IN_TYPES));

        MAP.put(FUNCTION_EXPECTED, "Expression ''{0}''{1} cannot be invoked as a function. " +
                                   "The function ''" + OperatorNameConventions.INVOKE.asString() + "()'' is not found",
                ELEMENT_TEXT, (type, context) -> {
                    if (KotlinTypeKt.isError(type)) return "";
                    return " of type '" + RENDER_TYPE.render(type, context) + "'";
                });
        MAP.put(FUNCTION_CALL_EXPECTED, "Function invocation ''{0}({1})'' expected", ELEMENT_TEXT,
                (hasValueParameters, context) -> hasValueParameters ? "..." : "");
        MAP.put(NON_TAIL_RECURSIVE_CALL, "Recursive call is not a tail call");
        MAP.put(TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED, "Tail recursion optimization inside try/catch/finally is not supported");

        MAP.put(RESULT_TYPE_MISMATCH, "{0} must return {1} but returns {2}", STRING, RENDER_TYPE, RENDER_TYPE);
        MAP.put(UNSAFE_INFIX_CALL,
                "Infix call corresponds to a dot-qualified call ''{0}.{1}({2})'' which is not allowed on a nullable receiver ''{0}''. " +
                "Use ''?.''-qualified call instead",
                ELEMENT_TEXT, STRING, ELEMENT_TEXT);
        MAP.put(UNSAFE_OPERATOR_CALL,
                "Operator call corresponds to a dot-qualified call ''{0}.{1}({2})'' which is not allowed on a nullable receiver ''{0}''.",
                ELEMENT_TEXT, STRING, ELEMENT_TEXT);

        MAP.put(OVERLOAD_RESOLUTION_AMBIGUITY, "Overload resolution ambiguity: {0}", AMBIGUOUS_CALLS);
        MAP.put(NONE_APPLICABLE, "None of the following functions can be called with the arguments supplied: {0}", AMBIGUOUS_CALLS);
        MAP.put(CANNOT_COMPLETE_RESOLVE, "Cannot choose among the following candidates without completing type inference: {0}", AMBIGUOUS_CALLS);
        MAP.put(UNRESOLVED_REFERENCE_WRONG_RECEIVER, "Unresolved reference. None of the following candidates is applicable because of receiver type mismatch: {0}", AMBIGUOUS_CALLS);

        MAP.put(NO_VALUE_FOR_PARAMETER, "No value passed for parameter ''{0}''", NAME);
        MAP.put(MISSING_RECEIVER, "A receiver of type {0} is required", RENDER_TYPE);
        MAP.put(NO_RECEIVER_ALLOWED, "No receiver can be passed to this function or property");
        MAP.put(ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM, "Assigning single elements to varargs in named form is deprecated");

        MAP.put(CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS, "Cannot create an instance of an abstract class");

        MAP.put(TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS, "Type inference failed: {0}", TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS_RENDERER);
        MAP.put(TYPE_INFERENCE_CANNOT_CAPTURE_TYPES, "Type inference failed: {0}", TYPE_INFERENCE_CANNOT_CAPTURE_TYPES_RENDERER);
        MAP.put(TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER, "Type inference failed: {0}", TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER_RENDERER);
        MAP.put(TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR, "Type inference failed: {0}", TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR_RENDERER);
        MAP.put(TYPE_INFERENCE_INCORPORATION_ERROR, "Type inference failed. Please try to specify type arguments explicitly.");
        MAP.put(TYPE_INFERENCE_ONLY_INPUT_TYPES, "Type inference failed. The value of the type parameter {0} should be mentioned in input types " +
                                                 "(argument types, receiver type or expected type). Try to specify it explicitly.", NAME);
        MAP.put(TYPE_INFERENCE_UPPER_BOUND_VIOLATED, "{0}", TYPE_INFERENCE_UPPER_BOUND_VIOLATED_RENDERER);
        MAP.put(TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH, "Type inference failed. Expected type mismatch: inferred type is {1} but {0} was expected", RENDER_TYPE, RENDER_TYPE);

        MAP.put(TYPE_INFERENCE_FAILED_ON_SPECIAL_CONSTRUCT, "Type inference for control flow expression failed. Please specify its type explicitly.");

        String wrongNumberOfTypeArguments = "{0,choice,0#No type arguments|1#One type argument|1<{0,number,integer} type arguments} expected";
        MAP.put(WRONG_NUMBER_OF_TYPE_ARGUMENTS, wrongNumberOfTypeArguments + " for {1}", null, COMPACT_WITHOUT_SUPERTYPES);

        MAP.put(
                OUTER_CLASS_ARGUMENTS_REQUIRED,
                "Type arguments should be specified for an outer {0}. Use full class name to specify them",
                RENDER_CLASS_OR_OBJECT_NAME);
        MAP.put(NO_TYPE_ARGUMENTS_ON_RHS, wrongNumberOfTypeArguments + ". Use ''{1}'' if you don''t want to pass type arguments", null, STRING);
        MAP.put(TYPE_ARGUMENTS_NOT_ALLOWED, "Type arguments are not allowed {0}", STRING);

        MAP.put(TYPE_PARAMETER_AS_REIFIED, "Cannot use ''{0}'' as reified type parameter. Use a class instead.", NAME);
        MAP.put(REIFIED_TYPE_PARAMETER_NO_INLINE, "Only type parameters of inline functions can be reified");
        MAP.put(REIFIED_TYPE_FORBIDDEN_SUBSTITUTION, "Cannot use ''{0}'' as reified type parameter", RENDER_TYPE);
        MAP.put(REIFIED_TYPE_UNSAFE_SUBSTITUTION, "It may be not safe to use ''{0}'' as an argument for a reified type parameter. Use a non-generic type or * if possible", RENDER_TYPE);
        MAP.put(TYPE_PARAMETERS_NOT_ALLOWED, "Type parameters are not allowed here");

        MAP.put(TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER, "Type parameter of a property must be used in its receiver type");

        MAP.put(SUPERTYPES_FOR_ANNOTATION_CLASS, "Annotation class cannot have supertypes");
        MAP.put(MISSING_VAL_ON_ANNOTATION_PARAMETER, "'val' keyword is missing on annotation parameter");
        MAP.put(VAR_ANNOTATION_PARAMETER, "An annotation parameter cannot be 'var'");
        MAP.put(ANNOTATION_CLASS_CONSTRUCTOR_CALL, "Annotation class cannot be instantiated");
        MAP.put(NOT_AN_ANNOTATION_CLASS, "''{0}'' is not an annotation class", NAME);
        MAP.put(ANNOTATION_CLASS_WITH_BODY, "Body is not allowed for annotation class");
        MAP.put(INVALID_TYPE_OF_ANNOTATION_MEMBER, "Invalid type of annotation member");
        MAP.put(NULLABLE_TYPE_OF_ANNOTATION_MEMBER, "An annotation parameter cannot be nullable");
        MAP.put(ANNOTATION_PARAMETER_MUST_BE_CONST, "An annotation parameter must be a compile-time constant");
        MAP.put(ANNOTATION_PARAMETER_MUST_BE_ENUM_CONST, "An enum annotation parameter must be a enum constant");
        MAP.put(ANNOTATION_PARAMETER_MUST_BE_KCLASS_LITERAL, "An annotation parameter must be a class literal (T::class)");
        MAP.put(ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT, "Default value of annotation parameter must be a compile-time constant");

        MAP.put(ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE,
                "Annotations on block-level expressions are being parsed differently depending on presence of a new line after them. " +
                "Use new line if whole block-level expression must be annotated or wrap annotated expression in parentheses");

        MAP.put(ANNOTATION_USED_AS_ANNOTATION_ARGUMENT, "An annotation can't be used as the annotations argument");

        MAP.put(CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT, "Const 'val' are only allowed on top level or in objects");
        MAP.put(CONST_VAL_WITH_DELEGATE, "Const 'val' should not have a delegate");
        MAP.put(CONST_VAL_WITH_GETTER, "Const 'val' should not have a getter");
        MAP.put(TYPE_CANT_BE_USED_FOR_CONST_VAL, "Const ''val'' has type ''{0}''. Only primitives and String are allowed", RENDER_TYPE);
        MAP.put(CONST_VAL_WITHOUT_INITIALIZER, "Const 'val' should have an initializer");
        MAP.put(CONST_VAL_WITH_NON_CONST_INITIALIZER, "Const 'val' initializer should be a constant value");
        MAP.put(NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION, "Only 'const val' can be used in constant expressions");

        MAP.put(DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE, "An overriding function is not allowed to specify default values for its parameters");


        String multipleDefaultsMessage = "More than one overridden descriptor declares a default value for ''{0}''. " +
                                         "As the compiler can not make sure these values agree, this is not allowed.";
        MAP.put(MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES, multipleDefaultsMessage, FQ_NAMES_IN_TYPES);
        MAP.put(MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE, multipleDefaultsMessage, FQ_NAMES_IN_TYPES);

        MAP.put(PARAMETER_NAME_CHANGED_ON_OVERRIDE, "The corresponding parameter in the supertype ''{0}'' is named ''{1}''. " +
                                                    "This may cause problems when calling this function with named arguments.", NAME, NAME);

        MAP.put(DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES,
                    "Names of the parameter #{1} conflict in the following members of supertypes: ''{0}''. " +
                    "This may cause problems when calling this function with named arguments.", commaSeparated(FQ_NAMES_IN_TYPES), TO_STRING);

        MAP.put(NAME_FOR_AMBIGUOUS_PARAMETER, "Named argument is not allowed for a parameter with an ambiguous name");

        MAP.put(DATA_CLASS_WITHOUT_PARAMETERS, "Data class must have at least one primary constructor parameter");
        MAP.put(DATA_CLASS_VARARG_PARAMETER, "Primary constructor vararg parameters are forbidden for data classes");
        MAP.put(DATA_CLASS_NOT_PROPERTY_PARAMETER, "Data class primary constructor must have only property (val / var) parameters");

        MAP.put(CATCH_PARAMETER_WITH_DEFAULT_VALUE, "Catch clause parameter may not have a default value");

        MAP.put(AMBIGUOUS_ANONYMOUS_TYPE_INFERRED, "Right-hand side has anonymous type. Please specify type explicitly", TO_STRING);
        MAP.put(KCLASS_WITH_NULLABLE_TYPE_PARAMETER_IN_SIGNATURE,
                "Declaration has an inconsistent return type. Please add upper bound Any for type parameter ''{0}'' or specify return type explicitly", NAME);

        MAP.put(EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED,
                "''{0}'' is a member and an extension at the same time. References to such elements are not allowed", NAME);
        MAP.put(CALLABLE_REFERENCE_LHS_NOT_A_CLASS, "Left-hand side of a callable reference cannot be a type parameter");
        MAP.put(CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR, "Annotation class cannot be instantiated");

        MAP.put(CLASS_LITERAL_LHS_NOT_A_CLASS, "Only classes are allowed on the left hand side of a class literal");
        MAP.put(ARRAY_CLASS_LITERAL_REQUIRES_ARGUMENT, "Array class literal requires a type argument, please specify one in angle brackets");
        MAP.put(NULLABLE_TYPE_IN_CLASS_LITERAL_LHS, "Type in a class literal must not be nullable");
        MAP.put(EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS, "Expression in a class literal has a nullable type ''{0}'', use !! to make the type non-nullable", RENDER_TYPE);

        //Inline
        MAP.put(NON_PUBLIC_CALL_FROM_PUBLIC_INLINE, "Public-API inline function cannot access non-public-API ''{0}''", SHORT_NAMES_IN_TYPES, SHORT_NAMES_IN_TYPES);
        MAP.put(PRIVATE_CLASS_MEMBER_FROM_INLINE, "Non-private inline function cannot access members of private classes: ''{0}''", SHORT_NAMES_IN_TYPES, SHORT_NAMES_IN_TYPES);
        MAP.put(NOT_YET_SUPPORTED_IN_INLINE, "{0} are not yet supported in inline functions", STRING);
        MAP.put(DECLARATION_CANT_BE_INLINED, "'inline' modifier is not allowed on virtual members. Only private or final members can be inlined");
        MAP.put(OVERRIDE_BY_INLINE, "Override by an inline function");
        MAP.put(REIFIED_TYPE_PARAMETER_IN_OVERRIDE, "Override by a function with reified type parameter");
        MAP.put(NOTHING_TO_INLINE, "Expected performance impact of inlining ''{0}'' can be insignificant. Inlining works best for functions with lambda parameters", SHORT_NAMES_IN_TYPES);
        MAP.put(USAGE_IS_NOT_INLINABLE, "Illegal usage of inline-parameter ''{0}'' in ''{1}''. Add ''noinline'' modifier to the parameter declaration", ELEMENT_TEXT, SHORT_NAMES_IN_TYPES);
        MAP.put(NULLABLE_INLINE_PARAMETER, "Inline-parameter ''{0}'' of ''{1}'' must not be nullable. Add ''noinline'' modifier to the parameter declaration or make its type not nullable", ELEMENT_TEXT, SHORT_NAMES_IN_TYPES);
        MAP.put(RECURSION_IN_INLINE, "Inline function ''{1}'' cannot be recursive", ELEMENT_TEXT, SHORT_NAMES_IN_TYPES);
        MAP.put(INLINE_PROPERTY_WITH_BACKING_FIELD, "Inline property cannot have backing field");
        MAP.put(NON_INTERNAL_PUBLISHED_API, "@PublishedApi annotation is only applicable for internal declaration");
        MAP.put(PROTECTED_CALL_FROM_PUBLIC_INLINE, "Protected function call from public-API inline function is deprecated", NAME);
        MAP.put(INVALID_DEFAULT_FUNCTIONAL_PARAMETER_FOR_INLINE, "Invalid default value for inline parameter: ''{0}''. Only lambdas, anonymous functions, and callable references are supported", ELEMENT_TEXT, SHORT_NAMES_IN_TYPES);
        MAP.put(NOT_SUPPORTED_INLINE_PARAMETER_IN_INLINE_PARAMETER_DEFAULT_VALUE, "Usage of inline parameter ''{0}'' in default value for another inline parameter is not supported", ELEMENT_TEXT, SHORT_NAMES_IN_TYPES);
        //Inline non locals
        MAP.put(NON_LOCAL_RETURN_NOT_ALLOWED, "Can''t inline ''{0}'' here: it may contain non-local returns. Add ''crossinline'' modifier to parameter declaration ''{0}''", ELEMENT_TEXT);
        MAP.put(INLINE_CALL_CYCLE, "The ''{0}'' invocation is a part of inline cycle", NAME);
        MAP.put(NON_LOCAL_RETURN_IN_DISABLED_INLINE, "Non-local returns are not allowed with inlining disabled");
        MAP.put(NON_LOCAL_SUSPENSION_POINT, "Suspension functions can be called only within coroutine body");
        MAP.put(ILLEGAL_SUSPEND_FUNCTION_CALL, "Suspend function ''{0}'' should be called only from a coroutine or another suspend function", NAME);
        MAP.put(ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL, "Restricted suspending functions can only invoke member or extension suspending functions on their restricted coroutine scope");

        MAP.put(PLUGIN_ERROR, "{0}: {1}", TO_STRING, TO_STRING);
        MAP.put(PLUGIN_WARNING, "{0}: {1}", TO_STRING, TO_STRING);
        MAP.put(PLUGIN_INFO, "{0}: {1}", TO_STRING, TO_STRING);

        MAP.setImmutable();

        for (Field field : Errors.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                try {
                    Object fieldValue = field.get(null);
                    if (fieldValue instanceof DiagnosticFactory) {
                        if (MAP.get((DiagnosticFactory<?>) fieldValue) == null) {
                            throw new IllegalStateException("No default diagnostic renderer is provided for " + ((DiagnosticFactory<?>)fieldValue).getName());
                        }
                    }
                }
                catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    private DefaultErrorMessages() {
    }
}
