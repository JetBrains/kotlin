/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.diagnostics.rendering;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.psi.JetTypeConstraint;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.renderer.Renderer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.diagnostics.Errors.DECLARATION_CANT_BE_INLINED;
import static org.jetbrains.jet.lang.diagnostics.rendering.Renderers.*;
import static org.jetbrains.jet.renderer.DescriptorRenderer.SHORT_NAMES_IN_TYPES;

public class DefaultErrorMessages {
    public static final DiagnosticFactoryToRendererMap MAP = new DiagnosticFactoryToRendererMap();
    public static final DiagnosticRenderer<Diagnostic> RENDERER = new DispatchingDiagnosticRenderer(MAP);

    static {
        MAP.put(EXCEPTION_WHILE_ANALYZING, "{0}", new Renderer<Throwable>() {
            @NotNull
            @Override
            public String render(@NotNull Throwable e) {
                return e.getClass().getSimpleName() + ": " + e.getMessage();
            }
        });

        MAP.put(UNRESOLVED_REFERENCE, "Unresolved reference: {0}", ELEMENT_TEXT);

        MAP.put(INVISIBLE_REFERENCE, "Cannot access ''{0}'': it is ''{1}'' in ''{2}''", NAME, TO_STRING, NAME);
        MAP.put(INVISIBLE_MEMBER, "Cannot access ''{0}'': it is ''{1}'' in ''{2}''", NAME, TO_STRING, NAME);

        MAP.put(REDECLARATION, "Redeclaration: {0}", NAME);
        MAP.put(NAME_SHADOWING, "Name shadowed: {0}", NAME);

        MAP.put(TYPE_MISMATCH, "Type mismatch: inferred type is {1} but {0} was expected", RENDER_TYPE, RENDER_TYPE);
        MAP.put(INCOMPATIBLE_MODIFIERS, "Incompatible modifiers: ''{0}''", new Renderer<Collection<JetKeywordToken>>() {
            @NotNull
            @Override
            public String render(@NotNull Collection<JetKeywordToken> tokens) {
                StringBuilder sb = new StringBuilder();
                for (JetKeywordToken token : tokens) {
                    if (sb.length() != 0) {
                        sb.append(" ");
                    }
                    sb.append(token.getValue());
                }
                return sb.toString();
            }
        });
        MAP.put(ILLEGAL_MODIFIER, "Illegal modifier ''{0}''", TO_STRING);

        MAP.put(REDUNDANT_MODIFIER, "Modifier ''{0}'' is redundant because ''{1}'' is present", TO_STRING, TO_STRING);
        MAP.put(ABSTRACT_MODIFIER_IN_TRAIT, "Modifier ''abstract'' is redundant in trait");
        MAP.put(OPEN_MODIFIER_IN_TRAIT, "Modifier ''open'' is redundant in trait");
        MAP.put(OPEN_MODIFIER_IN_ENUM, "Modifier ''open'' is not applicable for enum class");
        MAP.put(ILLEGAL_ENUM_ANNOTATION, "Annotation ''enum'' is only applicable for class");
        MAP.put(REDUNDANT_MODIFIER_IN_GETTER, "Visibility modifiers are redundant in getter");
        MAP.put(TRAIT_CAN_NOT_BE_FINAL, "Trait cannot be final");
        MAP.put(TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM,
                "Type checking has run into a recursive problem. Easiest workaround: specify types of your declarations explicitly"); // TODO: message
        MAP.put(RETURN_NOT_ALLOWED, "'return' is not allowed here");
        MAP.put(RETURN_NOT_ALLOWED_EXPLICIT_RETURN_TYPE_REQUIRED, "'return' is only allowed in function literals that have return types specified explicitly");
        MAP.put(PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE, "Projections are not allowed for immediate arguments of a supertype");
        MAP.put(LABEL_NAME_CLASH, "There is more than one label with such a name in this scope");
        MAP.put(EXPRESSION_EXPECTED_NAMESPACE_FOUND, "Expression expected, but a namespace name found");

        MAP.put(CANNOT_IMPORT_FROM_ELEMENT, "Cannot import from ''{0}''", NAME);
        MAP.put(CANNOT_BE_IMPORTED, "Cannot import ''{0}'', functions and properties can be imported only from packages", NAME);
        MAP.put(USELESS_HIDDEN_IMPORT, "Useless import, it is hidden further");
        MAP.put(USELESS_SIMPLE_IMPORT, "Useless import, does nothing");
        MAP.put(PLATFORM_CLASS_MAPPED_TO_KOTLIN, "This class shouldn''t be used in Kotlin. Use {0} instead.", CLASSES_OR_SEPARATED);

        MAP.put(CANNOT_INFER_PARAMETER_TYPE,
                "Cannot infer a type for this parameter. To specify it explicitly use the {(p : Type) => ...} notation");

        MAP.put(NO_BACKING_FIELD_ABSTRACT_PROPERTY, "This property doesn't have a backing field, because it's abstract");
        MAP.put(NO_BACKING_FIELD_CUSTOM_ACCESSORS,
                "This property doesn't have a backing field, because it has custom accessors without reference to the backing field");
        MAP.put(INACCESSIBLE_BACKING_FIELD, "The backing field is not accessible here");
        MAP.put(NOT_PROPERTY_BACKING_FIELD, "The referenced variable is not a property and doesn't have backing field");

        MAP.put(MIXING_NAMED_AND_POSITIONED_ARGUMENTS, "Mixing named and positioned arguments in not allowed");
        MAP.put(ARGUMENT_PASSED_TWICE, "An argument is already passed for this parameter");
        MAP.put(NAMED_PARAMETER_NOT_FOUND, "Cannot find a parameter with this name: {0}", ELEMENT_TEXT);
        MAP.put(VARARG_OUTSIDE_PARENTHESES, "Passing value as a vararg is only allowed inside a parenthesized argument list");
        MAP.put(NON_VARARG_SPREAD, "The spread operator (*foo) may only be applied in a vararg position");

        MAP.put(MANY_FUNCTION_LITERAL_ARGUMENTS, "Only one function literal is allowed outside a parenthesized argument list");
        MAP.put(PROPERTY_WITH_NO_TYPE_NO_INITIALIZER, "This property must either have a type annotation, be initialized or be delegated");
        MAP.put(VARIABLE_WITH_NO_TYPE_NO_INITIALIZER, "This variable must either have a type annotation or be initialized");

        MAP.put(INITIALIZER_REQUIRED_FOR_MULTIDECLARATION, "Initializer required for multi-declaration");
        MAP.put(COMPONENT_FUNCTION_MISSING, "Multi-declaration initializer of type {1} must have a ''{0}()'' function", TO_STRING, RENDER_TYPE);
        MAP.put(COMPONENT_FUNCTION_AMBIGUITY, "Function ''{0}()'' is ambiguous for this expression: {1}", TO_STRING, AMBIGUOUS_CALLS);
        MAP.put(COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH, "''{0}()'' function returns ''{1}'', but ''{2}'' is expected",
                                                                                    TO_STRING, RENDER_TYPE, RENDER_TYPE);

        MAP.put(ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS, "This property cannot be declared abstract");
        MAP.put(ABSTRACT_PROPERTY_WITH_INITIALIZER, "Property with initializer cannot be abstract");
        MAP.put(ABSTRACT_PROPERTY_WITH_GETTER, "Property with getter implementation cannot be abstract");
        MAP.put(ABSTRACT_PROPERTY_WITH_SETTER, "Property with setter implementation cannot be abstract");

        MAP.put(ABSTRACT_DELEGATED_PROPERTY, "Delegated property cannot be abstract");
        MAP.put(ACCESSOR_FOR_DELEGATED_PROPERTY, "Delegated property cannot have accessor");
        MAP.put(DELEGATED_PROPERTY_IN_TRAIT, "Delegated properties are not allowed in traits");
        MAP.put(LOCAL_VARIABLE_WITH_DELEGATE, "Local variables are not allowed to have delegates");

        MAP.put(PACKAGE_MEMBER_CANNOT_BE_PROTECTED, "Package member cannot be protected");

        MAP.put(GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY, "Getter visibility must be the same as property visibility");
        MAP.put(BACKING_FIELD_IN_TRAIT, "Property in a trait cannot have a backing field");
        MAP.put(MUST_BE_INITIALIZED, "Property must be initialized");
        MAP.put(MUST_BE_INITIALIZED_OR_BE_ABSTRACT, "Property must be initialized or be abstract");
        MAP.put(PROPERTY_INITIALIZER_IN_TRAIT, "Property initializers are not allowed in traits");
        MAP.put(FINAL_PROPERTY_IN_TRAIT, "Abstract property in trait cannot be final");
        MAP.put(PROPERTY_INITIALIZER_NO_BACKING_FIELD, "Initializer is not allowed here because this property has no backing field");
        MAP.put(ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS, "Abstract property ''{0}'' in non-abstract class ''{1}''", NAME, NAME);
        MAP.put(ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS, "Abstract function ''{0}'' in non-abstract class ''{1}''", NAME, NAME);
        MAP.put(ABSTRACT_FUNCTION_WITH_BODY, "A function ''{0}'' with body cannot be abstract", NAME);
        MAP.put(NON_ABSTRACT_FUNCTION_WITH_NO_BODY, "Function ''{0}'' without a body must be abstract", NAME);
        MAP.put(FINAL_FUNCTION_WITH_NO_BODY, "Function ''{0}'' without body cannot be final", NAME);

        MAP.put(NON_MEMBER_FUNCTION_NO_BODY, "Function ''{0}'' must have a body", NAME);
        MAP.put(NON_FINAL_MEMBER_IN_FINAL_CLASS, "\"open\" has no effect in a final class");

        MAP.put(PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE, "Public or protected member should have specified type");

        MAP.put(PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT, "Projections are not allowed on type arguments of functions and properties");
        MAP.put(SUPERTYPE_NOT_INITIALIZED, "This type has a constructor, and thus must be initialized here");
        MAP.put(NOTHING_TO_OVERRIDE, "''{0}'' overrides nothing", NAME);
        MAP.put(VIRTUAL_MEMBER_HIDDEN, "''{0}'' hides member of supertype ''{2}'' and needs ''override'' modifier", NAME, NAME, NAME);

        MAP.put(DATA_CLASS_OVERRIDE_CONFLICT, "Function ''{0}'' generated for the data class conflicts with member of supertype ''{1}''", NAME, NAME);

        MAP.put(CANNOT_OVERRIDE_INVISIBLE_MEMBER, "''{0}'' cannot has no access to ''{1}'' in class {2}, so it cannot override it",
                DescriptorRenderer.TEXT, DescriptorRenderer.TEXT, DescriptorRenderer.TEXT);
        MAP.put(CANNOT_INFER_VISIBILITY, "Cannot infer visibility. Please specify it explicitly");

        MAP.put(ENUM_ENTRY_SHOULD_BE_INITIALIZED, "Missing delegation specifier ''{0}''", NAME);
        MAP.put(ENUM_ENTRY_ILLEGAL_TYPE, "The type constructor of enum entry should be ''{0}''", NAME);

        MAP.put(UNINITIALIZED_VARIABLE, "Variable ''{0}'' must be initialized", NAME);
        MAP.put(UNINITIALIZED_PARAMETER, "Parameter ''{0}'' is uninitialized here", NAME);
        MAP.put(UNUSED_VARIABLE, "Variable ''{0}'' is never used", NAME);
        MAP.put(UNUSED_PARAMETER, "Parameter ''{0}'' is never used", NAME);
        MAP.put(ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE, "Variable ''{0}'' is assigned but never accessed", NAME);
        MAP.put(VARIABLE_WITH_REDUNDANT_INITIALIZER, "Variable ''{0}'' initializer is redundant", NAME);
        MAP.put(UNUSED_VALUE, "The value ''{0}'' assigned to ''{1}'' is never used", ELEMENT_TEXT, TO_STRING);
        MAP.put(UNUSED_CHANGED_VALUE, "The value changed at ''{0}'' is never used", ELEMENT_TEXT);
        MAP.put(UNUSED_EXPRESSION, "The expression is unused");
        MAP.put(UNUSED_FUNCTION_LITERAL, "The function literal is unused. If you mean block, you can use 'run { ... }'");

        MAP.put(VAL_REASSIGNMENT, "Val cannot be reassigned", NAME);
        MAP.put(SETTER_PROJECTED_OUT, "Setter for ''{0}'' is removed by type projection", NAME);
        MAP.put(INVISIBLE_SETTER, "Cannot assign to ''{0}'': the setter is ''{1}'' in ''{2}''", NAME, TO_STRING, NAME);
        MAP.put(INITIALIZATION_BEFORE_DECLARATION, "Variable cannot be initialized before declaration", NAME);
        MAP.put(VARIABLE_EXPECTED, "Variable expected");

        MAP.put(VAL_OR_VAR_ON_LOOP_PARAMETER, "''{0}'' on loop parameter is not allowed", TO_STRING);
        MAP.put(VAL_OR_VAR_ON_FUN_PARAMETER, "''{0}'' on function parameter is not allowed", TO_STRING);
        MAP.put(VAL_OR_VAR_ON_CATCH_PARAMETER, "''{0}'' on catch parameter is not allowed", TO_STRING);

        MAP.put(INITIALIZATION_USING_BACKING_FIELD_CUSTOM_SETTER,
                "This property has a custom setter, so initialization using backing field required", NAME);
        MAP.put(INITIALIZATION_USING_BACKING_FIELD_OPEN_SETTER,
                "Setter of this property can be overridden, so initialization using backing field required", NAME);

        MAP.put(UNREACHABLE_CODE, "Unreachable code");

        MAP.put(MANY_CLASS_OBJECTS, "Only one class object is allowed per class");
        MAP.put(CLASS_OBJECT_NOT_ALLOWED, "A class object is not allowed here");
        MAP.put(DELEGATION_IN_TRAIT, "Traits cannot use delegation");
        MAP.put(DELEGATION_NOT_TO_TRAIT, "Only traits can be delegated to");
        MAP.put(NO_CONSTRUCTOR, "This class does not have a constructor");
        MAP.put(NOT_A_CLASS, "Not a class");
        MAP.put(ILLEGAL_ESCAPE_SEQUENCE, "Illegal escape sequence");

        MAP.put(LOCAL_EXTENSION_PROPERTY, "Local extension properties are not allowed");
        MAP.put(LOCAL_VARIABLE_WITH_GETTER, "Local variables are not allowed to have getters");
        MAP.put(LOCAL_VARIABLE_WITH_SETTER, "Local variables are not allowed to have setters");
        MAP.put(VAL_WITH_SETTER, "A 'val'-property cannot have a setter");

        MAP.put(NO_GET_METHOD, "No get method providing array access");
        MAP.put(NO_SET_METHOD, "No set method providing array access");

        MAP.put(INC_DEC_SHOULD_NOT_RETURN_UNIT, "Functions inc(), dec() shouldn't return Unit to be used by operators ++, --");
        MAP.put(ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT, "Function ''{0}'' should return Unit to be used by corresponding operator ''{1}''",
                NAME, ELEMENT_TEXT);
        MAP.put(ASSIGN_OPERATOR_AMBIGUITY, "Assignment operators ambiguity: {0}", AMBIGUOUS_CALLS);

        MAP.put(EQUALS_MISSING, "No method 'equals(jet.Any?) : jet.Boolean' available");
        MAP.put(ASSIGNMENT_IN_EXPRESSION_CONTEXT, "Assignments are not expressions, and only expressions are allowed in this context");
        MAP.put(NAMESPACE_IS_NOT_AN_EXPRESSION, "'namespace' is not an expression, it can only be used on the left-hand side of a dot ('.')");
        MAP.put(SUPER_IS_NOT_AN_EXPRESSION, "''{0}'' is not an expression, it can only be used on the left-hand side of a dot ('.')", TO_STRING);
        MAP.put(DECLARATION_IN_ILLEGAL_CONTEXT, "Declarations are not allowed in this position");
        MAP.put(SETTER_PARAMETER_WITH_DEFAULT_VALUE, "Setter parameters cannot have default values");
        MAP.put(NO_THIS, "'this' is not defined in this context");
        MAP.put(SUPER_NOT_AVAILABLE, "No supertypes are accessible in this context");
        MAP.put(SUPERCLASS_NOT_ACCESSIBLE_FROM_TRAIT, "Superclass is not accessible from trait");
        MAP.put(AMBIGUOUS_SUPER, "Many supertypes available, please specify the one you mean in angle brackets, e.g. 'super<Foo>'");
        MAP.put(ABSTRACT_SUPER_CALL, "Abstract member cannot be accessed directly");
        MAP.put(NOT_A_SUPERTYPE, "Not a supertype");
        MAP.put(TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER, "Type arguments do not need to be specified in a 'super' qualifier");
        MAP.put(USELESS_CAST_STATIC_ASSERT_IS_FINE, "No cast needed, use ':' instead");
        MAP.put(USELESS_CAST, "No cast needed");
        MAP.put(CAST_NEVER_SUCCEEDS, "This cast can never succeed");
        MAP.put(USELESS_NULLABLE_CHECK, "Non-null type is checked for instance of nullable type");
        MAP.put(WRONG_SETTER_PARAMETER_TYPE, "Setter parameter type must be equal to the type of the property, i.e. ''{0}''", RENDER_TYPE, RENDER_TYPE);
        MAP.put(WRONG_GETTER_RETURN_TYPE, "Getter return type must be equal to the type of the property, i.e. ''{0}''", RENDER_TYPE, RENDER_TYPE);
        MAP.put(NO_CLASS_OBJECT, "Please specify constructor invocation; classifier ''{0}'' does not have a class object", NAME);
        MAP.put(TYPE_PARAMETER_IS_NOT_AN_EXPRESSION, "Type parameter ''{0}'' is not an expression", NAME);
        MAP.put(TYPE_PARAMETER_ON_LHS_OF_DOT, "Type parameter ''{0}'' cannot have or inherit a class object, so it cannot be on the left hand side of dot", NAME);
        MAP.put(NO_GENERICS_IN_SUPERTYPE_SPECIFIER, "Generic arguments of the base type must be specified");

        MAP.put(INACCESSIBLE_OUTER_CLASS_EXPRESSION, "Expression is inaccessible from a nested class ''{0}'', use ''inner'' keyword to make the class inner", NAME);
        MAP.put(NESTED_CLASS_NOT_ALLOWED, "Nested class is not allowed here, use ''inner'' keyword to make the class inner");

        MAP.put(HAS_NEXT_MISSING, "hasNext() cannot be called on iterator() of type ''{0}''", RENDER_TYPE);
        MAP.put(HAS_NEXT_FUNCTION_AMBIGUITY, "hasNext() is ambiguous for iterator() of type ''{0}''", RENDER_TYPE);
        MAP.put(HAS_NEXT_FUNCTION_NONE_APPLICABLE, "None of the hasNext() functions is applicable for iterator() of type ''{0}''", RENDER_TYPE);
        MAP.put(HAS_NEXT_FUNCTION_TYPE_MISMATCH, "The ''iterator().hasNext()'' function of the loop range must return jet.Boolean, but returns {0}",
                RENDER_TYPE);

        MAP.put(NEXT_MISSING, "next() cannot be called on iterator() of type ''{0}''", RENDER_TYPE);
        MAP.put(NEXT_AMBIGUITY, "next() is ambiguous for iterator() of type ''{0}''", RENDER_TYPE);
        MAP.put(NEXT_NONE_APPLICABLE, "None of the next() functions is applicable for iterator() of type ''{0}''", RENDER_TYPE);

        MAP.put(ITERATOR_MISSING, "For-loop range must have an iterator() method");
        MAP.put(ITERATOR_AMBIGUITY, "Method ''iterator()'' is ambiguous for this expression: {0}", AMBIGUOUS_CALLS);

        MAP.put(DELEGATE_SPECIAL_FUNCTION_MISSING, "Missing ''{0}'' method on delegate of type ''{1}''", TO_STRING, RENDER_TYPE);
        MAP.put(DELEGATE_SPECIAL_FUNCTION_AMBIGUITY, "Overload resolution ambiguity on method ''{0}'': {1}",  TO_STRING, AMBIGUOUS_CALLS);
        MAP.put(DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE, "Property delegate must have a ''{0}'' method. None of the following functions is suitable: {1}", TO_STRING,  AMBIGUOUS_CALLS);
        MAP.put(DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH, "The ''{0}'' function of property delegate is expected to return ''{1}'', but returns ''{2}''",
                TO_STRING, RENDER_TYPE, RENDER_TYPE);

        MAP.put(COMPARE_TO_TYPE_MISMATCH, "''compareTo()'' must return jet.Int, but returns {0}", RENDER_TYPE);
        MAP.put(CALLEE_NOT_A_FUNCTION, "Expecting a function type, but found {0}", RENDER_TYPE);

        MAP.put(RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY,
                "Returns are not allowed for functions with expression body. Use block body in '{...}'");
        MAP.put(NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY, "A 'return' expression required in a function with a block body ('{...}')");
        MAP.put(RETURN_TYPE_MISMATCH, "This function must return a value of type {0}", RENDER_TYPE);
        MAP.put(EXPECTED_TYPE_MISMATCH, "Expected a value of type {0}", RENDER_TYPE);
        MAP.put(ASSIGNMENT_TYPE_MISMATCH,
                "Expected a value of type {0}. Assignment operation is not an expression, so it does not return any value", RENDER_TYPE);

        MAP.put(EXPECTED_PARAMETER_TYPE_MISMATCH, "Expected parameter of type {0}", RENDER_TYPE);
        MAP.put(EXPECTED_RETURN_TYPE_MISMATCH, "Expected return type {0}", RENDER_TYPE);
        MAP.put(EXPECTED_PARAMETERS_NUMBER_MISMATCH, "Expected {0,choice,0#no parameters|1#one parameter of type|1<{0,number,integer} parameters of types} {1}", null, RENDER_COLLECTION_OF_TYPES);

        MAP.put(IMPLICIT_CAST_TO_UNIT_OR_ANY, "Type was casted to ''{0}''. Please specify ''{0}'' as expected type, if you mean such cast",
                RENDER_TYPE);
        MAP.put(EXPRESSION_EXPECTED, "{0} is not an expression, and only expressions are allowed here", new Renderer<JetExpression>() {
            @NotNull
            @Override
            public String render(@NotNull JetExpression expression) {
                String expressionType = expression.toString();
                return expressionType.substring(0, 1) +
                       expressionType.substring(1).toLowerCase();
            }
        });

        MAP.put(UPPER_BOUND_VIOLATED, "Type argument is not within its bounds: should be subtype of ''{0}''", RENDER_TYPE, RENDER_TYPE);
        MAP.put(FINAL_CLASS_OBJECT_UPPER_BOUND, "''{0}'' is a final type, and thus a class object cannot extend it", RENDER_TYPE);
        MAP.put(FINAL_UPPER_BOUND, "''{0}'' is a final type, and thus a value of the type parameter is predetermined", RENDER_TYPE);
        MAP.put(USELESS_ELVIS, "Elvis operator (?:) always returns the left operand of non-nullable type {0}", RENDER_TYPE);
        MAP.put(CONFLICTING_UPPER_BOUNDS, "Upper bounds of {0} have empty intersection", NAME);
        MAP.put(CONFLICTING_CLASS_OBJECT_UPPER_BOUNDS, "Class object upper bounds of {0} have empty intersection", NAME);

        MAP.put(TOO_MANY_ARGUMENTS, "Too many arguments for {0}", DescriptorRenderer.TEXT);

        MAP.put(CONSTANT_EXPECTED_TYPE_MISMATCH, "An {0} literal does not conform to the expected type {1}", TO_STRING, RENDER_TYPE);
        MAP.put(INTEGER_OVERFLOW, "This operation has led to an overflow");
        MAP.put(INT_LITERAL_OUT_OF_RANGE, "The value is out of range");
        MAP.put(WRONG_LONG_SUFFIX, "Use 'L' instead of 'l'");
        MAP.put(FLOAT_LITERAL_OUT_OF_RANGE, "The value is out of range");
        MAP.put(INCORRECT_CHARACTER_LITERAL, "Incorrect character literal");
        MAP.put(EMPTY_CHARACTER_LITERAL, "Empty character literal");
        MAP.put(TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL, "Too many characters in a character literal ''{0}''", ELEMENT_TEXT);
        MAP.put(ILLEGAL_ESCAPE, "Illegal escape: ''{0}''", ELEMENT_TEXT);
        MAP.put(NULL_FOR_NONNULL_TYPE, "Null can not be a value of a non-null type {0}", RENDER_TYPE);

        MAP.put(ELSE_MISPLACED_IN_WHEN, "'else' entry must be the last one in a when-expression");

        MAP.put(NO_ELSE_IN_WHEN, "'when' expression must contain 'else' branch");
        MAP.put(TYPE_MISMATCH_IN_RANGE, "Type mismatch: incompatible types of range and element checked in it");
        MAP.put(CYCLIC_INHERITANCE_HIERARCHY, "There's a cycle in the inheritance hierarchy for this type");

        MAP.put(MANY_CLASSES_IN_SUPERTYPE_LIST, "Only one class may appear in a supertype list");
        MAP.put(SUPERTYPE_NOT_A_CLASS_OR_TRAIT, "Only classes and traits may serve as supertypes");
        MAP.put(SUPERTYPE_INITIALIZED_IN_TRAIT, "Traits cannot initialize supertypes");
        MAP.put(CLASS_IN_SUPERTYPE_FOR_ENUM, "Enum class cannot inherit from classes");
        MAP.put(CONSTRUCTOR_IN_TRAIT, "A trait may not have a constructor");
        MAP.put(SUPERTYPE_APPEARS_TWICE, "A supertype appears twice");
        MAP.put(FINAL_SUPERTYPE, "This type is final, so it cannot be inherited from");
        MAP.put(SINGLETON_IN_SUPERTYPE, "Cannot inherit from a singleton");

        MAP.put(ILLEGAL_SELECTOR, "Expression ''{0}'' cannot be a selector (occur after a dot)", TO_STRING);

        MAP.put(NO_TAIL_CALLS_FOUND, "A function is marked as tail-recursive but no tail calls are found");
        MAP.put(VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION, "A type annotation is required on a value parameter");
        MAP.put(BREAK_OR_CONTINUE_OUTSIDE_A_LOOP, "'break' and 'continue' are only allowed inside a loop");
        MAP.put(NOT_A_LOOP_LABEL, "The label ''{0}'' does not denote a loop", TO_STRING);
        MAP.put(NOT_A_RETURN_LABEL, "The label ''{0}'' does not reference to a context from which we can return", TO_STRING);

        MAP.put(ANONYMOUS_INITIALIZER_IN_TRAIT, "Anonymous initializers are not allowed in traits");
        MAP.put(NULLABLE_SUPERTYPE, "A supertype cannot be nullable");
        MAP.put(REDUNDANT_NULLABLE, "Redundant '?'");
        MAP.put(BASE_WITH_NULLABLE_UPPER_BOUND, "''{0}'' has a nullable upper bound. " +
                                                "This means that a value of this type may be null. " +
                                                "Using ''{0}?'' is likely to mislead the reader", RENDER_TYPE);
        MAP.put(UNSAFE_CALL, "Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type {0}", RENDER_TYPE);
        MAP.put(AMBIGUOUS_LABEL, "Ambiguous label");
        MAP.put(UNSUPPORTED, "Unsupported [{0}]", TO_STRING);
        MAP.put(UNNECESSARY_SAFE_CALL, "Unnecessary safe call on a non-null receiver of type {0}", RENDER_TYPE);
        MAP.put(UNNECESSARY_NOT_NULL_ASSERTION, "Unnecessary non-null assertion (!!) on a non-null receiver of type {0}", RENDER_TYPE);
        MAP.put(NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER, "{0} does not refer to a type parameter of {1}", new Renderer<JetTypeConstraint>() {
            @NotNull
            @Override
            public String render(@NotNull JetTypeConstraint typeConstraint) {
                //noinspection ConstantConditions
                return typeConstraint.getSubjectTypeParameterName().getReferencedName();
            }
        }, NAME);
        MAP.put(AUTOCAST_IMPOSSIBLE, "Automatic cast to ''{0}'' is impossible, because ''{1}'' could have changed since the is-check", RENDER_TYPE,
                NAME);

        MAP.put(VARIANCE_ON_TYPE_PARAMETER_OF_FUNCTION_OR_PROPERTY, "Variance annotations are only allowed for type parameters of classes and traits");
        MAP.put(REDUNDANT_PROJECTION, "Projection is redundant: the corresponding type parameter of {0} has the same variance", NAME);
        MAP.put(CONFLICTING_PROJECTION, "Projection is conflicting with variance of the corresponding type parameter of {0}. Remove the projection or replace it with ''*''", NAME);

        MAP.put(TYPE_MISMATCH_IN_FOR_LOOP, "The loop iterates over values of type {0} but the parameter is declared to be {1}", RENDER_TYPE,
                RENDER_TYPE);
        MAP.put(TYPE_MISMATCH_IN_CONDITION, "Condition must be of type jet.Boolean, but is of type {0}", RENDER_TYPE);
        MAP.put(INCOMPATIBLE_TYPES, "Incompatible types: {0} and {1}", RENDER_TYPE, RENDER_TYPE);
        MAP.put(EXPECTED_CONDITION, "Expected condition of jet.Boolean type");

        MAP.put(CANNOT_CHECK_FOR_ERASED, "Cannot check for instance of erased type: {0}", RENDER_TYPE);
        MAP.put(UNCHECKED_CAST, "Unchecked cast: {0} to {1}", RENDER_TYPE, RENDER_TYPE);

        MAP.put(INCONSISTENT_TYPE_PARAMETER_VALUES, "Type parameter {0} of ''{1}'' has inconsistent values: {2}", NAME, NAME, RENDER_COLLECTION_OF_TYPES);

        MAP.put(EQUALITY_NOT_APPLICABLE, "Operator ''{0}'' cannot be applied to ''{1}'' and ''{2}''", new Renderer<JetSimpleNameExpression>() {
            @NotNull
            @Override
            public String render(@NotNull JetSimpleNameExpression nameExpression) {
                //noinspection ConstantConditions
                return nameExpression.getReferencedName();
            }
        }, RENDER_TYPE, RENDER_TYPE);

        MAP.put(SENSELESS_COMPARISON, "Condition ''{0}'' is always ''{1}''", ELEMENT_TEXT, TO_STRING);
        MAP.put(SENSELESS_NULL_IN_WHEN, "Expression under 'when' is never equal to null");

        MAP.put(OVERRIDING_FINAL_MEMBER, "''{0}'' in ''{1}'' is final and cannot be overridden", NAME, NAME);
        MAP.put(CANNOT_WEAKEN_ACCESS_PRIVILEGE, "Cannot weaken access privilege ''{0}'' for ''{1}'' in ''{2}''", TO_STRING, NAME, NAME);
        MAP.put(CANNOT_CHANGE_ACCESS_PRIVILEGE, "Cannot change access privilege ''{0}'' for ''{1}'' in ''{2}''", TO_STRING, NAME, NAME);

        MAP.put(RETURN_TYPE_MISMATCH_ON_OVERRIDE, "Return type of ''{0}'' is not a subtype of the return type of overridden member {1}",
                NAME, DescriptorRenderer.TEXT);

        MAP.put(PROPERTY_TYPE_MISMATCH_ON_OVERRIDE, "Type of ''{0}'' doesn't match to the type of overridden var-property {1}",
                NAME, DescriptorRenderer.TEXT);

        MAP.put(VAR_OVERRIDDEN_BY_VAL, "Var-property {0} cannot be overridden by val-property {1}", DescriptorRenderer.TEXT,
                DescriptorRenderer.TEXT);

        MAP.put(ABSTRACT_MEMBER_NOT_IMPLEMENTED, "{0} must be declared abstract or implement abstract member {1}", RENDER_CLASS_OR_OBJECT,
                DescriptorRenderer.TEXT);

        MAP.put(MANY_IMPL_MEMBER_NOT_IMPLEMENTED, "{0} must override {1} because it inherits many implementations of it",
                RENDER_CLASS_OR_OBJECT, DescriptorRenderer.TEXT);

        MAP.put(CONFLICTING_OVERLOADS, "{1} is already defined in ''{0}''", DescriptorRenderer.TEXT, TO_STRING);

        MAP.put(FUNCTION_EXPECTED, "Expression ''{0}''{1} cannot be invoked as a function", ELEMENT_TEXT, new Renderer<JetType>() {
            @NotNull
            @Override
            public String render(@NotNull JetType type) {
                if (type.isError()) return "";
                return " of type '" + type.toString() + "'";
            }
        });
        MAP.put(FUNCTION_CALL_EXPECTED, "Function invocation ''{0}({1})'' expected", ELEMENT_TEXT,new Renderer<Boolean>() {
            @NotNull
            @Override
            public String render(@NotNull Boolean hasValueParameters) {
                return hasValueParameters ? "..." : "";
            }
        });
        MAP.put(NON_TAIL_RECURSIVE_CALL, "Recursive call is not a tail call");
        MAP.put(TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED, "Tail recursion optimization inside try/catch/finally is not supported");

        MAP.put(RESULT_TYPE_MISMATCH, "{0} must return {1} but returns {2}", TO_STRING, RENDER_TYPE, RENDER_TYPE);
        MAP.put(UNSAFE_INFIX_CALL,
                "Infix call corresponds to a dot-qualified call ''{0}.{1}({2})'' which is not allowed on a nullable receiver ''{0}''. " +
                "Use '?.'-qualified call instead",
                TO_STRING, TO_STRING, TO_STRING);

        MAP.put(OVERLOAD_RESOLUTION_AMBIGUITY, "Overload resolution ambiguity: {0}", AMBIGUOUS_CALLS);
        MAP.put(NONE_APPLICABLE, "None of the following functions can be called with the arguments supplied: {0}", AMBIGUOUS_CALLS);
        MAP.put(CANNOT_COMPLETE_RESOLVE, "Cannot choose among the following candidates without completing type inference: {0}", AMBIGUOUS_CALLS);
        MAP.put(UNRESOLVED_REFERENCE_WRONG_RECEIVER, "Unresolved reference. None of the following candidates is applicable because of receiver type mismatch: {0}", AMBIGUOUS_CALLS);

        MAP.put(NO_VALUE_FOR_PARAMETER, "No value passed for parameter {0}", NAME);
        MAP.put(MISSING_RECEIVER, "A receiver of type {0} is required", RENDER_TYPE);
        MAP.put(NO_RECEIVER_ADMITTED, "No receiver can be passed to this function or property");

        MAP.put(CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS, "Cannot create an instance of an abstract class");

        MAP.put(TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS, "Type inference failed: {0}", TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS_RENDERER);
        MAP.put(TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER, "Type inference failed: {0}", TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER_RENDERER);
        MAP.put(TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH, "Type inference failed: {0}", TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH_RENDERER);
        MAP.put(TYPE_INFERENCE_UPPER_BOUND_VIOLATED, "{0}", TYPE_INFERENCE_UPPER_BOUND_VIOLATED_RENDERER);
        MAP.put(TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH, "Type inference failed. Expected type mismatch: found: {1} required: {0}", RENDER_TYPE, RENDER_TYPE);

        MAP.put(WRONG_NUMBER_OF_TYPE_ARGUMENTS, "{0,choice,0#No type arguments|1#Type argument|1<{0,number,integer} type arguments} expected", null);
        MAP.put(NO_TYPE_ARGUMENTS_ON_RHS, "{0,choice,0#No type arguments|1#Type argument|1<{0,number,integer} type arguments} expected. " +
                                                           "Use ''{1}'' if you don''t want to pass type arguments", null, TO_STRING);

        MAP.put(DANGLING_FUNCTION_LITERAL_ARGUMENT_SUSPECTED,
                "This expression is treated as an argument to the function call on the previous line. " +
                "Separate it with a semicolon (;) if it is not intended to be an argument.");

        MAP.put(TYPE_PARAMETER_AS_REIFIED, "Cannot use ''{0}'' as reified type parameter. Use a class instead.", NAME);

        MAP.put(NOT_AN_ANNOTATION_CLASS, "''{0}'' is not an annotation class", TO_STRING);
        MAP.put(ANNOTATION_CLASS_WITH_BODY, "Body is not allowed for annotation class");
        MAP.put(INVALID_TYPE_OF_ANNOTATION_MEMBER, "Invalid type of annotation member");
        MAP.put(NULLABLE_TYPE_OF_ANNOTATION_MEMBER, "An annotation parameter cannot be nullable");
        MAP.put(ILLEGAL_ANNOTATION_KEYWORD, "''annotation'' keyword is only applicable for class");
        MAP.put(ANNOTATION_PARAMETER_MUST_BE_CONST, "An annotation parameter must be a compile-time constant");
        MAP.put(ANNOTATION_PARAMETER_MUST_BE_ENUM_CONST, "An enum annotation parameter must be a enum constant");
        MAP.put(ANNOTATION_PARAMETER_MUST_BE_CLASS_LITERAL, "An annotation parameter must be a class literal");

        MAP.put(DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE, "An overriding function is not allowed to specify default values for its parameters");


        String multipleDefaultsMessage = "More than one overridden descriptor declares a default value for ''{0}''. " +
                                         "As the compiler can not make sure these values agree, this is not allowed.";
        MAP.put(MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES, multipleDefaultsMessage, TO_STRING);
        MAP.put(MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE, multipleDefaultsMessage, TO_STRING);

        MAP.put(PARAMETER_NAME_CHANGED_ON_OVERRIDE, "The corresponding parameter in the supertype ''{0}'' is named ''{1}''. " +
                                                    "This may cause problems when calling this function with named arguments.", NAME, NAME);

        MAP.put(DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES,
                    "Names of the parameter #{1} conflict in the following members of supertypes: ''{0}''" +
                    "This may cause problems when calling this function with named arguments.", commaSeparated(TO_STRING), TO_STRING);

        MAP.put(AMBIGUOUS_ANONYMOUS_TYPE_INFERRED, "Right-hand side has anonymous type. Please specify type explicitly", TO_STRING);

        MAP.put(EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED,
                "''{0}'' is a member and an extension at the same time. References to such elements are not allowed", TO_STRING);
        MAP.put(CALLABLE_REFERENCE_LHS_NOT_A_CLASS, "Callable reference left-hand side cannot be a type parameter");

        //Inline
        MAP.put(INVISIBLE_MEMBER_FROM_INLINE, "Cannot access effectively non-public-api ''{0}'' member from effectively public-api ''{1}''", SHORT_NAMES_IN_TYPES, SHORT_NAMES_IN_TYPES);
        MAP.put(NOT_YET_SUPPORTED_IN_INLINE, "''{0}'' construction not yet supported in inline functions", ELEMENT_TEXT, SHORT_NAMES_IN_TYPES);
        MAP.put(DECLARATION_CANT_BE_INLINED, "Inline annotation could be present only on nonvirtual members (private or final)");
        MAP.put(NOTHING_TO_INLINE, "There are no parameters of Function types to be inlined in ''{0}''", SHORT_NAMES_IN_TYPES);
        MAP.put(USAGE_IS_NOT_INLINABLE, "Illegal usage of inline-parameter ''{0}'' in ''{1}''. Annotate the parameter with [noinline]", ELEMENT_TEXT, SHORT_NAMES_IN_TYPES);
        MAP.put(NULLABLE_INLINE_PARAMETER, "Inline-parameter ''{0}'' of ''{1}'' must not be nullable. Annotate the parameter with [noinline] or make not nullable", ELEMENT_TEXT, SHORT_NAMES_IN_TYPES);
        MAP.put(RECURSION_IN_INLINE, "Inline-function ''{1}'' can't be recursive", ELEMENT_TEXT, SHORT_NAMES_IN_TYPES);

        MAP.setImmutable();

        for (Field field : Errors.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                try {
                    Object fieldValue = field.get(null);
                    if (fieldValue instanceof DiagnosticFactory) {
                        if (MAP.get((DiagnosticFactory) fieldValue) == null) {
                            throw new IllegalStateException("No default diagnostic renderer is provided for " + ((DiagnosticFactory)fieldValue).getName());
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
