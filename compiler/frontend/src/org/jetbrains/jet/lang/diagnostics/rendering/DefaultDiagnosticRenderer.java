/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.inference.SolutionStatus;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.diagnostics.Renderers.*;

/**
 * @author Evgeny Gerashchenko
 * @since 4/12/12
 */
public class DefaultDiagnosticRenderer implements DiagnosticRenderer<Diagnostic> {
    public static final DefaultDiagnosticRenderer INSTANCE = new DefaultDiagnosticRenderer();

    private final Map<AbstractDiagnosticFactory, DiagnosticRenderer<?>> map =
            new HashMap<AbstractDiagnosticFactory, DiagnosticRenderer<?>>();

    private DefaultDiagnosticRenderer() {
        map.put(EXCEPTION_WHILE_ANALYZING, new DiagnosticWithParameters1Renderer<Throwable>("{0}", new Renderer<Throwable>() {
            @NotNull
            @Override
            public String render(@NotNull Throwable e) {
                return e.getClass().getSimpleName() + ": " + e.getMessage();
            }
        }));

        map.put(UNRESOLVED_REFERENCE, new UnresolvedReferenceDiagnosticRenderer("Unresolved reference: "));

        map.put(INVISIBLE_REFERENCE,
                new DiagnosticWithParameters2Renderer<DeclarationDescriptor, DeclarationDescriptor>("Cannot access ''{0}'' in ''{1}''",
                                                                                                    NAME, NAME));
        map.put(INVISIBLE_MEMBER,
                new DiagnosticWithParameters2Renderer<DeclarationDescriptor, DeclarationDescriptor>("Cannot access ''{0}'' in ''{1}''",
                                                                                                    NAME, NAME));

        map.put(REDECLARATION, new RedeclarationDiagnosticRenderer("Redeclaration: "));
        map.put(NAME_SHADOWING, new RedeclarationDiagnosticRenderer("Name shadowed: "));

        map.put(TYPE_MISMATCH,
                new DiagnosticWithParameters2Renderer<JetType, JetType>("Type mismatch: inferred type is {1} but {0} was expected",
                                                                        RENDER_TYPE, RENDER_TYPE));
        map.put(INCOMPATIBLE_MODIFIERS,
                new DiagnosticWithParameters1Renderer<Collection<JetKeywordToken>>("Incompatible modifiers: ''{0}''",
                                                                                   new Renderer<Collection<JetKeywordToken>>() {
                                                                                       @NotNull
                                                                                       @Override
                                                                                       public String render(@NotNull Collection<JetKeywordToken> tokens) {
                                                                                           StringBuilder sb = new StringBuilder();
                                                                                           for (Iterator<JetKeywordToken> iterator =
                                                                                                        tokens.iterator();
                                                                                                iterator.hasNext(); ) {
                                                                                               JetKeywordToken modifier = iterator.next();
                                                                                               sb.append(modifier.getValue());
                                                                                               if (iterator.hasNext()) {
                                                                                                   sb.append(" ");
                                                                                               }
                                                                                           }
                                                                                           return sb.toString();
                                                                                       }
                                                                                   }
                ));
        map.put(ILLEGAL_MODIFIER, new DiagnosticWithParameters1Renderer<JetKeywordToken>("Illegal modifier ''{0}''", TO_STRING));

        map.put(REDUNDANT_MODIFIER,
                new DiagnosticWithParameters2Renderer<JetKeywordToken, JetKeywordToken>("Modifier {0} is redundant because {1} is present",
                                                                                        TO_STRING, TO_STRING));
        map.put(ABSTRACT_MODIFIER_IN_TRAIT, new SimpleDiagnosticRenderer("Modifier ''{0}'' is redundant in trait"));
        map.put(OPEN_MODIFIER_IN_TRAIT, new SimpleDiagnosticRenderer("Modifier ''{0}'' is redundant in trait"));
        map.put(REDUNDANT_MODIFIER_IN_GETTER, new SimpleDiagnosticRenderer("Visibility modifiers are redundant in getter"));
        map.put(TRAIT_CAN_NOT_BE_FINAL, new SimpleDiagnosticRenderer("Trait can not be final"));
        map.put(TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM, new SimpleDiagnosticRenderer(
                "Type checking has run into a recursive problem. Easiest workaround: specify types of your declarations explicitly")); // TODO: message
        map.put(RETURN_NOT_ALLOWED, new SimpleDiagnosticRenderer("'return' is not allowed here"));
        map.put(PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE,
                new SimpleDiagnosticRenderer("Projections are not allowed for immediate arguments of a supertype"));
        map.put(LABEL_NAME_CLASH, new SimpleDiagnosticRenderer("There is more than one label with such a name in this scope"));
        map.put(EXPRESSION_EXPECTED_NAMESPACE_FOUND, new SimpleDiagnosticRenderer("Expression expected, but a namespace name found"));

        map.put(CANNOT_IMPORT_FROM_ELEMENT,
                new DiagnosticWithParameters1Renderer<DeclarationDescriptor>("Cannot import from ''{0}''", NAME));
        map.put(CANNOT_BE_IMPORTED, new DiagnosticWithParameters1Renderer<DeclarationDescriptor>(
                "Cannot import ''{0}'', functions and properties can be imported only from packages", NAME));
        map.put(USELESS_HIDDEN_IMPORT, new SimpleDiagnosticRenderer("Useless import, it is hidden further"));
        map.put(USELESS_SIMPLE_IMPORT, new SimpleDiagnosticRenderer("Useless import, does nothing"));

        map.put(CANNOT_INFER_PARAMETER_TYPE, new SimpleDiagnosticRenderer(
                "Cannot infer a type for this parameter. To specify it explicitly use the {(p : Type) => ...} notation"));

        map.put(NO_BACKING_FIELD_ABSTRACT_PROPERTY,
                new SimpleDiagnosticRenderer("This property doesn't have a backing field, because it's abstract"));
        map.put(NO_BACKING_FIELD_CUSTOM_ACCESSORS, new SimpleDiagnosticRenderer(
                "This property doesn't have a backing field, because it has custom accessors without reference to the backing field"));
        map.put(INACCESSIBLE_BACKING_FIELD, new SimpleDiagnosticRenderer("The backing field is not accessible here"));
        map.put(NOT_PROPERTY_BACKING_FIELD,
                new SimpleDiagnosticRenderer("The referenced variable is not a property and doesn't have backing field"));

        map.put(MIXING_NAMED_AND_POSITIONED_ARGUMENTS,
                new SimpleDiagnosticRenderer("Mixing named and positioned arguments in not allowed"));
        map.put(ARGUMENT_PASSED_TWICE, new SimpleDiagnosticRenderer("An argument is already passed for this parameter"));
        map.put(NAMED_PARAMETER_NOT_FOUND, new UnresolvedReferenceDiagnosticRenderer("Cannot find a parameter with this name: "));
        map.put(VARARG_OUTSIDE_PARENTHESES,
                new SimpleDiagnosticRenderer("Passing value as a vararg is only allowed inside a parenthesized argument list"));
        map.put(NON_VARARG_SPREAD, new SimpleDiagnosticRenderer("The spread operator (*foo) may only be applied in a vararg position"));

        map.put(MANY_FUNCTION_LITERAL_ARGUMENTS,
                new SimpleDiagnosticRenderer("Only one function literal is allowed outside a parenthesized argument list"));
        map.put(PROPERTY_WITH_NO_TYPE_NO_INITIALIZER,
                new SimpleDiagnosticRenderer("This property must either have a type annotation or be initialized"));

        map.put(ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS,
                new SimpleDiagnosticRenderer("This property cannot be declared abstract"));
        map.put(ABSTRACT_PROPERTY_NOT_IN_CLASS,
                new SimpleDiagnosticRenderer("A property may be abstract only when defined in a class or trait"));
        map.put(ABSTRACT_PROPERTY_WITH_INITIALIZER, new SimpleDiagnosticRenderer("Property with initializer cannot be abstract"));
        map.put(ABSTRACT_PROPERTY_WITH_GETTER, new SimpleDiagnosticRenderer("Property with getter implementation cannot be abstract"));
        map.put(ABSTRACT_PROPERTY_WITH_SETTER, new SimpleDiagnosticRenderer("Property with setter implementation cannot be abstract"));

        map.put(PACKAGE_MEMBER_CANNOT_BE_PROTECTED, new SimpleDiagnosticRenderer("Package member cannot be protected"));

        map.put(GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY,
                new SimpleDiagnosticRenderer("Getter visibility must be the same as property visibility"));
        map.put(BACKING_FIELD_IN_TRAIT, new SimpleDiagnosticRenderer("Property in a trait cannot have a backing field"));
        map.put(MUST_BE_INITIALIZED, new SimpleDiagnosticRenderer("Property must be initialized"));
        map.put(MUST_BE_INITIALIZED_OR_BE_ABSTRACT, new SimpleDiagnosticRenderer("Property must be initialized or be abstract"));
        map.put(PROPERTY_INITIALIZER_IN_TRAIT, new SimpleDiagnosticRenderer("Property initializers are not allowed in traits"));
        map.put(PROPERTY_INITIALIZER_NO_BACKING_FIELD,
                new SimpleDiagnosticRenderer("Initializer is not allowed here because this property has no backing field"));
        map.put(ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS,
                new DiagnosticWithParameters3Renderer<String, ClassDescriptor, JetClass>("Abstract property {0} in non-abstract class {1}",
                                                                                         TO_STRING, TO_STRING, TO_STRING));
        map.put(ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS,
                new DiagnosticWithParameters3Renderer<String, ClassDescriptor, JetClass>("Abstract function {0} in non-abstract class {1}",
                                                                                         TO_STRING, TO_STRING, TO_STRING));
        map.put(ABSTRACT_FUNCTION_WITH_BODY,
                new DiagnosticWithParameters1Renderer<SimpleFunctionDescriptor>("A function {0} with body cannot be abstract", TO_STRING));
        map.put(NON_ABSTRACT_FUNCTION_WITH_NO_BODY,
                new DiagnosticWithParameters1Renderer<SimpleFunctionDescriptor>("Method {0} without a body must be abstract", TO_STRING));
        map.put(NON_MEMBER_ABSTRACT_FUNCTION, new DiagnosticWithParameters1Renderer<SimpleFunctionDescriptor>(
                "Function {0} is not a class or trait member and cannot be abstract", TO_STRING));

        map.put(NON_MEMBER_FUNCTION_NO_BODY,
                new DiagnosticWithParameters1Renderer<SimpleFunctionDescriptor>("Function {0} must have a body", TO_STRING));
        map.put(NON_FINAL_MEMBER_IN_FINAL_CLASS, new SimpleDiagnosticRenderer("Non final member in a final class"));

        map.put(PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE, new SimpleDiagnosticRenderer("Public or protected member should specify a type"));

        map.put(PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT, new SimpleDiagnosticRenderer(
                "Projections are not allowed on type arguments of functions and properties")); // TODO : better positioning
        map.put(SUPERTYPE_NOT_INITIALIZED, new SimpleDiagnosticRenderer("This type has a constructor, and thus must be initialized here"));
        map.put(SUPERTYPE_NOT_INITIALIZED_DEFAULT, new SimpleDiagnosticRenderer("Constructor invocation should be explicitly specified"));
        map.put(SECONDARY_CONSTRUCTOR_BUT_NO_PRIMARY,
                new SimpleDiagnosticRenderer("A secondary constructor may appear only in a class that has a primary constructor"));
        map.put(SECONDARY_CONSTRUCTOR_NO_INITIALIZER_LIST,
                new SimpleDiagnosticRenderer("Secondary constructors must have an initializer list"));
        map.put(BY_IN_SECONDARY_CONSTRUCTOR, new SimpleDiagnosticRenderer("'by'-clause is only supported for primary constructors"));
        map.put(INITIALIZER_WITH_NO_ARGUMENTS, new SimpleDiagnosticRenderer("Constructor arguments required"));
        map.put(MANY_CALLS_TO_THIS, new SimpleDiagnosticRenderer("Only one call to 'this(...)' is allowed"));
        map.put(NOTHING_TO_OVERRIDE,
                new DiagnosticWithParameters1Renderer<CallableMemberDescriptor>("{0} overrides nothing", DescriptorRenderer.TEXT));
        map.put(VIRTUAL_MEMBER_HIDDEN,
                new DiagnosticWithParameters3Renderer<CallableMemberDescriptor, CallableMemberDescriptor, DeclarationDescriptor>(
                        "''{0}'' hides ''{1}'' in class {2} and needs 'override' modifier", DescriptorRenderer.TEXT,
                        DescriptorRenderer.TEXT, DescriptorRenderer.TEXT));

        map.put(ENUM_ENTRY_SHOULD_BE_INITIALIZED,
                new DiagnosticWithParameters1Renderer<ClassDescriptor>("Missing delegation specifier ''{0}''", NAME));
        map.put(ENUM_ENTRY_ILLEGAL_TYPE,
                new DiagnosticWithParameters1Renderer<ClassDescriptor>("The type constructor of enum entry should be ''{0}''", NAME));

        map.put(UNINITIALIZED_VARIABLE,
                new DiagnosticWithParameters1Renderer<VariableDescriptor>("Variable ''{0}'' must be initialized", NAME));
        map.put(UNINITIALIZED_PARAMETER,
                new DiagnosticWithParameters1Renderer<ValueParameterDescriptor>("Parameter ''{0}'' is uninitialized here", NAME));
        map.put(UNUSED_VARIABLE, new DiagnosticWithParameters1Renderer<VariableDescriptor>("Variable ''{0}'' is never used", NAME));
        map.put(UNUSED_PARAMETER, new DiagnosticWithParameters1Renderer<VariableDescriptor>("Parameter ''{0}'' is never used", NAME));
        map.put(ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE,
                new DiagnosticWithParameters1Renderer<DeclarationDescriptor>("Variable ''{0}'' is assigned but never accessed", NAME));
        map.put(VARIABLE_WITH_REDUNDANT_INITIALIZER,
                new DiagnosticWithParameters1Renderer<DeclarationDescriptor>("Variable ''{0}'' initializer is redundant", NAME));
        map.put(UNUSED_VALUE, new DiagnosticWithParameters2Renderer<JetElement, DeclarationDescriptor>(
                "The value ''{0}'' assigned to ''{1}'' is never used", ELEMENT_TEXT, TO_STRING));
        map.put(UNUSED_CHANGED_VALUE,
                new DiagnosticWithParameters1Renderer<JetElement>("The value changed at ''{0}'' is never used", ELEMENT_TEXT));
        map.put(UNUSED_EXPRESSION, new SimpleDiagnosticRenderer("The expression is unused"));
        map.put(UNUSED_FUNCTION_LITERAL,
                new SimpleDiagnosticRenderer("The function literal is unused. If you mean block, you can use 'run { ... }'"));

        map.put(VAL_REASSIGNMENT, new DiagnosticWithParameters1Renderer<DeclarationDescriptor>("Val can not be reassigned", NAME));
        map.put(INITIALIZATION_BEFORE_DECLARATION,
                new DiagnosticWithParameters1Renderer<DeclarationDescriptor>("Variable cannot be initialized before declaration", NAME));
        map.put(VARIABLE_EXPECTED, new SimpleDiagnosticRenderer("Variable expected"));

        map.put(INITIALIZATION_USING_BACKING_FIELD_CUSTOM_SETTER, new DiagnosticWithParameters1Renderer<DeclarationDescriptor>(
                "This property has a custom setter, so initialization using backing field required", NAME));
        map.put(INITIALIZATION_USING_BACKING_FIELD_OPEN_SETTER, new DiagnosticWithParameters1Renderer<DeclarationDescriptor>(
                "Setter of this property can be overridden, so initialization using backing field required", NAME));

        map.put(FUNCTION_PARAMETERS_OF_INLINE_FUNCTION,
                new DiagnosticWithParameters1Renderer<DeclarationDescriptor>("Function parameters of inline function can only be invoked",
                                                                             NAME));

        map.put(UNREACHABLE_CODE, new SimpleDiagnosticRenderer("Unreachable code"));

        map.put(MANY_CLASS_OBJECTS, new SimpleDiagnosticRenderer("Only one class object is allowed per class"));
        map.put(CLASS_OBJECT_NOT_ALLOWED, new SimpleDiagnosticRenderer("A class object is not allowed here"));
        map.put(DELEGATION_IN_TRAIT, new SimpleDiagnosticRenderer("Traits cannot use delegation"));
        map.put(DELEGATION_NOT_TO_TRAIT, new SimpleDiagnosticRenderer("Only traits can be delegated to"));
        map.put(NO_CONSTRUCTOR, new SimpleDiagnosticRenderer("This class does not have a constructor"));
        map.put(NOT_A_CLASS, new SimpleDiagnosticRenderer("Not a class"));
        map.put(ILLEGAL_ESCAPE_SEQUENCE, new SimpleDiagnosticRenderer("Illegal escape sequence"));

        map.put(LOCAL_EXTENSION_PROPERTY, new SimpleDiagnosticRenderer("Local extension properties are not allowed"));
        map.put(LOCAL_VARIABLE_WITH_GETTER, new SimpleDiagnosticRenderer("Local variables are not allowed to have getters"));
        map.put(LOCAL_VARIABLE_WITH_SETTER, new SimpleDiagnosticRenderer("Local variables are not allowed to have setters"));
        map.put(VAL_WITH_SETTER, new SimpleDiagnosticRenderer("A 'val'-property cannot have a setter"));

        map.put(NO_GET_METHOD, new SimpleDiagnosticRenderer("No get method providing array access"));
        map.put(NO_SET_METHOD, new SimpleDiagnosticRenderer("No set method providing array access"));

        map.put(INC_DEC_SHOULD_NOT_RETURN_UNIT,
                new SimpleDiagnosticRenderer("Functions inc(), dec() shouldn't return Unit to be used by operators ++, --"));
        map.put(ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT,
                new DiagnosticWithParameters2Renderer<DeclarationDescriptor, JetSimpleNameExpression>(
                        "Function ''{0}'' should return Unit to be used by corresponding operator ''{1}''", NAME, ELEMENT_TEXT));
        map.put(ASSIGN_OPERATOR_AMBIGUITY, new AmbiguousDescriptorDiagnosticRenderer("Assignment operators ambiguity: {0}"));

        map.put(EQUALS_MISSING, new SimpleDiagnosticRenderer("No method 'equals(Any?) : Boolean' available"));
        map.put(ASSIGNMENT_IN_EXPRESSION_CONTEXT,
                new SimpleDiagnosticRenderer("Assignments are not expressions, and only expressions are allowed in this context"));
        map.put(NAMESPACE_IS_NOT_AN_EXPRESSION,
                new SimpleDiagnosticRenderer("'namespace' is not an expression, it can only be used on the left-hand side of a dot ('.')"));
        map.put(SUPER_IS_NOT_AN_EXPRESSION, new DiagnosticWithParameters1Renderer<String>(
                "{0} is not an expression, it can only be used on the left-hand side of a dot ('.')", TO_STRING));
        map.put(DECLARATION_IN_ILLEGAL_CONTEXT, new SimpleDiagnosticRenderer("Declarations are not allowed in this position"));
        map.put(SETTER_PARAMETER_WITH_DEFAULT_VALUE, new SimpleDiagnosticRenderer("Setter parameters can not have default values"));
        map.put(NO_THIS, new SimpleDiagnosticRenderer("'this' is not defined in this context"));
        map.put(SUPER_NOT_AVAILABLE, new SimpleDiagnosticRenderer("No supertypes are accessible in this context"));
        map.put(AMBIGUOUS_SUPER, new SimpleDiagnosticRenderer(
                "Many supertypes available, please specify the one you mean in angle brackets, e.g. 'super<Foo>'"));
        map.put(ABSTRACT_SUPER_CALL, new SimpleDiagnosticRenderer("Abstract member cannot be accessed directly"));
        map.put(NOT_A_SUPERTYPE, new SimpleDiagnosticRenderer("Not a supertype"));
        map.put(TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER,
                new SimpleDiagnosticRenderer("Type arguments do not need to be specified in a 'super' qualifier"));
        map.put(USELESS_CAST_STATIC_ASSERT_IS_FINE, new SimpleDiagnosticRenderer("No cast needed, use ':' instead"));
        map.put(USELESS_CAST, new SimpleDiagnosticRenderer("No cast needed"));
        map.put(CAST_NEVER_SUCCEEDS, new SimpleDiagnosticRenderer("This cast can never succeed"));
        map.put(WRONG_SETTER_PARAMETER_TYPE,
                new DiagnosticWithParameters1Renderer<JetType>("Setter parameter type must be equal to the type of the property, i.e. {0}",
                                                               RENDER_TYPE));
        map.put(WRONG_GETTER_RETURN_TYPE,
                new DiagnosticWithParameters1Renderer<JetType>("Getter return type must be equal to the type of the property, i.e. {0}",
                                                               RENDER_TYPE));
        map.put(NO_CLASS_OBJECT, new DiagnosticWithParameters1Renderer<ClassifierDescriptor>(
                "Please specify constructor invocation; classifier {0} does not have a class object", NAME));
        map.put(NO_GENERICS_IN_SUPERTYPE_SPECIFIER, new SimpleDiagnosticRenderer("Generic arguments of the base type must be specified"));

        map.put(HAS_NEXT_PROPERTY_AND_FUNCTION_AMBIGUITY,
                new SimpleDiagnosticRenderer("An ambiguity between 'iterator().hasNext()' function and 'iterator().hasNext' property"));
        map.put(HAS_NEXT_MISSING, new SimpleDiagnosticRenderer(
                "Loop range must have an 'iterator().hasNext()' function or an 'iterator().hasNext' property"));
        map.put(HAS_NEXT_FUNCTION_AMBIGUITY,
                new SimpleDiagnosticRenderer("Function 'iterator().hasNext()' is ambiguous for this expression"));
        map.put(HAS_NEXT_MUST_BE_READABLE,
                new SimpleDiagnosticRenderer("The 'iterator().hasNext' property of the loop range must be readable"));
        map.put(HAS_NEXT_PROPERTY_TYPE_MISMATCH, new DiagnosticWithParameters1Renderer<JetType>(
                "The 'iterator().hasNext' property of the loop range must return Boolean, but returns {0}", RENDER_TYPE));
        map.put(HAS_NEXT_FUNCTION_TYPE_MISMATCH, new DiagnosticWithParameters1Renderer<JetType>(
                "The 'iterator().hasNext()' function of the loop range must return Boolean, but returns {0}", RENDER_TYPE));
        map.put(NEXT_AMBIGUITY, new SimpleDiagnosticRenderer("Function 'iterator().next()' is ambiguous for this expression"));
        map.put(NEXT_MISSING, new SimpleDiagnosticRenderer("Loop range must have an 'iterator().next()' function"));
        map.put(ITERATOR_MISSING, new SimpleDiagnosticRenderer("For-loop range must have an iterator() method"));
        map.put(ITERATOR_AMBIGUITY, new AmbiguousDescriptorDiagnosticRenderer("Method 'iterator()' is ambiguous for this expression: {0}"));

        map.put(COMPARE_TO_TYPE_MISMATCH,
                new DiagnosticWithParameters1Renderer<JetType>("compareTo() must return Int, but returns {0}", RENDER_TYPE));
        map.put(CALLEE_NOT_A_FUNCTION,
                new DiagnosticWithParameters1Renderer<JetType>("Expecting a function type, but found {0}", RENDER_TYPE));

        map.put(RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY,
                new SimpleDiagnosticRenderer("Returns are not allowed for functions with expression body. Use block body in '{...}'"));
        map.put(NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY,
                new SimpleDiagnosticRenderer("A 'return' expression required in a function with a block body ('{...}')"));
        map.put(RETURN_TYPE_MISMATCH,
                new DiagnosticWithParameters1Renderer<JetType>("This function must return a value of type {0}", RENDER_TYPE));
        map.put(EXPECTED_TYPE_MISMATCH, new DiagnosticWithParameters1Renderer<JetType>("Expected a value of type {0}", RENDER_TYPE));
        map.put(ASSIGNMENT_TYPE_MISMATCH, new DiagnosticWithParameters1Renderer<JetType>(
                "Expected a value of type {0}. Assignment operation is not an expression, so it does not return any value", RENDER_TYPE));
        map.put(IMPLICIT_CAST_TO_UNIT_OR_ANY, new DiagnosticWithParameters1Renderer<JetType>(
                "Type was casted to ''{0}''. Please specify ''{0}'' as expected type, if you mean such cast", RENDER_TYPE));
        map.put(EXPRESSION_EXPECTED,
                new DiagnosticWithParameters1Renderer<JetExpression>("{0} is not an expression, and only expression are allowed here",
                                                                     new Renderer<JetExpression>() {
                                                                         @NotNull
                                                                         @Override
                                                                         public String render(@NotNull JetExpression expression) {
                                                                             String expressionType = expression.toString();
                                                                             return expressionType.substring(0, 1) +
                                                                                    expressionType.substring(1).toLowerCase();
                                                                         }
                                                                     }));

        map.put(UPPER_BOUND_VIOLATED,
                new DiagnosticWithParameters1Renderer<JetType>("An upper bound {0} is violated", RENDER_TYPE)); // TODO : Message
        map.put(FINAL_CLASS_OBJECT_UPPER_BOUND,
                new DiagnosticWithParameters1Renderer<JetType>("{0} is a final type, and thus a class object cannot extend it",
                                                               RENDER_TYPE));
        map.put(FINAL_UPPER_BOUND, new DiagnosticWithParameters1Renderer<JetType>(
                "{0} is a final type, and thus a value of the type parameter is predetermined", RENDER_TYPE));
        map.put(USELESS_ELVIS, new DiagnosticWithParameters1Renderer<JetType>(
                "Elvis operator (?:) always returns the left operand of non-nullable type {0}", RENDER_TYPE));
        map.put(CONFLICTING_UPPER_BOUNDS,
                new DiagnosticWithParameters1Renderer<TypeParameterDescriptor>("Upper bounds of {0} have empty intersection", NAME));
        map.put(CONFLICTING_CLASS_OBJECT_UPPER_BOUNDS,
                new DiagnosticWithParameters1Renderer<TypeParameterDescriptor>("Class object upper bounds of {0} have empty intersection",
                                                                               NAME));

        map.put(TOO_MANY_ARGUMENTS, new DiagnosticWithParameters1Renderer<CallableDescriptor>("Too many arguments for {0}", TO_STRING));
        map.put(ERROR_COMPILE_TIME_VALUE, new DiagnosticWithParameters1Renderer<String>("{0}", TO_STRING));

        map.put(ELSE_MISPLACED_IN_WHEN, new SimpleDiagnosticRenderer("'else' entry must be the last one in a when-expression"));

        map.put(NO_ELSE_IN_WHEN, new SimpleDiagnosticRenderer("'when' expression must contain 'else' branch"));
        map.put(TYPE_MISMATCH_IN_RANGE,
                new SimpleDiagnosticRenderer("Type mismatch: incompatible types of range and element checked in it"));
        map.put(CYCLIC_INHERITANCE_HIERARCHY, new SimpleDiagnosticRenderer("There's a cycle in the inheritance hierarchy for this type"));

        map.put(MANY_CLASSES_IN_SUPERTYPE_LIST, new SimpleDiagnosticRenderer("Only one class may appear in a supertype list"));
        map.put(SUPERTYPE_NOT_A_CLASS_OR_TRAIT, new SimpleDiagnosticRenderer("Only classes and traits may serve as supertypes"));
        map.put(SUPERTYPE_INITIALIZED_IN_TRAIT, new SimpleDiagnosticRenderer("Traits cannot initialize supertypes"));
        map.put(CONSTRUCTOR_IN_TRAIT, new SimpleDiagnosticRenderer("A trait may not have a constructor"));
        map.put(SECONDARY_CONSTRUCTORS_ARE_NOT_SUPPORTED, new SimpleDiagnosticRenderer("Secondary constructors are not supported"));
        map.put(SUPERTYPE_APPEARS_TWICE, new SimpleDiagnosticRenderer("A supertype appears twice"));
        map.put(FINAL_SUPERTYPE, new SimpleDiagnosticRenderer("This type is final, so it cannot be inherited from"));

        map.put(ILLEGAL_SELECTOR,
                new DiagnosticWithParameters1Renderer<String>("Expression ''{0}'' cannot be a selector (occur after a dot)", TO_STRING));

        map.put(VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION,
                new SimpleDiagnosticRenderer("A type annotation is required on a value parameter"));
        map.put(BREAK_OR_CONTINUE_OUTSIDE_A_LOOP, new SimpleDiagnosticRenderer("'break' and 'continue' are only allowed inside a loop"));
        map.put(NOT_A_LOOP_LABEL, new DiagnosticWithParameters1Renderer<String>("The label ''{0}'' does not denote a loop", TO_STRING));
        map.put(NOT_A_RETURN_LABEL,
                new DiagnosticWithParameters1Renderer<String>("The label ''{0}'' does not reference to a context from which we can return",
                                                              TO_STRING));

        map.put(ANONYMOUS_INITIALIZER_WITHOUT_CONSTRUCTOR,
                new SimpleDiagnosticRenderer("Anonymous initializers are only allowed in the presence of a primary constructor"));
        map.put(NULLABLE_SUPERTYPE, new SimpleDiagnosticRenderer("A supertype cannot be nullable"));
        map.put(UNSAFE_CALL,
                new DiagnosticWithParameters1Renderer<JetType>("Only safe calls (?.) are allowed on a nullable receiver of type {0}",
                                                               RENDER_TYPE));
        map.put(AMBIGUOUS_LABEL, new SimpleDiagnosticRenderer("Ambiguous label"));
        map.put(UNSUPPORTED, new DiagnosticWithParameters1Renderer<String>("Unsupported [{0}]", TO_STRING));
        map.put(UNNECESSARY_SAFE_CALL,
                new DiagnosticWithParameters1Renderer<JetType>("Unnecessary safe call on a non-null receiver of type {0}", RENDER_TYPE));
        map.put(UNNECESSARY_NOT_NULL_ASSERTION,
                new DiagnosticWithParameters1Renderer<JetType>("Unnecessary non-null assertion (!!) on a non-null receiver of type {0}",
                                                               RENDER_TYPE));
        map.put(NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER,
                new DiagnosticWithParameters2Renderer<JetTypeConstraint, JetTypeParameterListOwner>(
                        "{0} does not refer to a type parameter of {1}", new Renderer<JetTypeConstraint>() {
                    @NotNull
                    @Override
                    public String render(@NotNull JetTypeConstraint typeConstraint) {
                        //noinspection ConstantConditions
                        return typeConstraint.getSubjectTypeParameterName().getReferencedName();
                    }
                }, NAME));
        map.put(AUTOCAST_IMPOSSIBLE, new DiagnosticWithParameters2Renderer<JetType, String>(
                "Automatic cast to {0} is impossible, because {1} could have changed since the is-check", RENDER_TYPE, NAME));

        map.put(TYPE_MISMATCH_IN_FOR_LOOP, new DiagnosticWithParameters2Renderer<JetType, JetType>(
                "The loop iterates over values of type {0} but the parameter is declared to be {1}", RENDER_TYPE, RENDER_TYPE));
        map.put(TYPE_MISMATCH_IN_CONDITION,
                new DiagnosticWithParameters1Renderer<JetType>("Condition must be of type Boolean, but was of type {0}", RENDER_TYPE));
        map.put(TYPE_MISMATCH_IN_TUPLE_PATTERN, new DiagnosticWithParameters2Renderer<JetType, Integer>(
                "Type mismatch: subject is of type {0} but the pattern is of type Tuple{1}", RENDER_TYPE, TO_STRING)); // TODO: message
        map.put(TYPE_MISMATCH_IN_BINDING_PATTERN,
                new DiagnosticWithParameters2Renderer<JetType, JetType>("{0} must be a supertype of {1}. Use 'is' to match against {0}",
                                                                        RENDER_TYPE, RENDER_TYPE));
        map.put(INCOMPATIBLE_TYPES,
                new DiagnosticWithParameters2Renderer<JetType, JetType>("Incompatible types: {0} and {1}", RENDER_TYPE, RENDER_TYPE));
        map.put(EXPECTED_CONDITION, new SimpleDiagnosticRenderer("Expected condition of Boolean type"));

        map.put(CANNOT_CHECK_FOR_ERASED,
                new DiagnosticWithParameters1Renderer<JetType>("Cannot check for instance of erased type: {0}", RENDER_TYPE));
        map.put(UNCHECKED_CAST,
                new DiagnosticWithParameters2Renderer<JetType, JetType>("Unchecked cast: {0} to {1}", RENDER_TYPE, RENDER_TYPE));

        map.put(INCONSISTENT_TYPE_PARAMETER_VALUES,
                new DiagnosticWithParameters3Renderer<TypeParameterDescriptor, ClassDescriptor, Collection<JetType>>(
                        "Type parameter {0} of {1} has inconsistent values: {2}", NAME, DescriptorRenderer.TEXT,
                        new Renderer<Collection<JetType>>() {
                            @NotNull
                            @Override
                            public String render(@NotNull Collection<JetType> types) {
                                StringBuilder builder = new StringBuilder();
                                for (Iterator<JetType> iterator = types.iterator(); iterator.hasNext(); ) {
                                    JetType jetType = iterator.next();
                                    builder.append(jetType);
                                    if (iterator.hasNext()) {
                                        builder.append(", ");
                                    }
                                }
                                return builder.toString();
                            }
                        }));

        map.put(EQUALITY_NOT_APPLICABLE, new DiagnosticWithParameters3Renderer<JetSimpleNameExpression, JetType, JetType>(
                "Operator {0} cannot be applied to {1} and {2}", new Renderer<JetSimpleNameExpression>() {
            @NotNull
            @Override
            public String render(@NotNull JetSimpleNameExpression nameExpression) {
                //noinspection ConstantConditions
                return nameExpression.getReferencedName();
            }
        }, TO_STRING, TO_STRING));

        map.put(OVERRIDING_FINAL_MEMBER, new DiagnosticWithParameters2Renderer<CallableMemberDescriptor, DeclarationDescriptor>(
                "''{0}'' in ''{1}'' is final and cannot be overridden", NAME, NAME));
        map.put(CANNOT_WEAKEN_ACCESS_PRIVILEGE,
                new DiagnosticWithParameters3Renderer<Visibility, CallableMemberDescriptor, DeclarationDescriptor>(
                        "Cannot weaken access privilege ''{0}'' for ''{1}'' in ''{2}''", TO_STRING, NAME, NAME));
        map.put(CANNOT_CHANGE_ACCESS_PRIVILEGE,
                new DiagnosticWithParameters3Renderer<Visibility, CallableMemberDescriptor, DeclarationDescriptor>(
                        "Cannot change access privilege ''{0}'' for ''{1}'' in ''{2}''", TO_STRING, NAME, NAME));

        map.put(RETURN_TYPE_MISMATCH_ON_OVERRIDE, new DiagnosticWithParameters2Renderer<CallableMemberDescriptor, CallableMemberDescriptor>(
                "Return type of {0} is not a subtype of the return type overridden member {1}", DescriptorRenderer.TEXT,
                DescriptorRenderer.TEXT));

        map.put(VAR_OVERRIDDEN_BY_VAL, new DiagnosticWithParameters2Renderer<PropertyDescriptor, PropertyDescriptor>(
                "Var-property {0} cannot be overridden by val-property {1}", DescriptorRenderer.TEXT, DescriptorRenderer.TEXT));

        map.put(ABSTRACT_MEMBER_NOT_IMPLEMENTED, new DiagnosticWithParameters2Renderer<JetClassOrObject, CallableMemberDescriptor>(
                "{0} must be declared abstract or implement abstract member {1}", RENDER_CLASS_OR_OBJECT, DescriptorRenderer.TEXT));

        map.put(MANY_IMPL_MEMBER_NOT_IMPLEMENTED, new DiagnosticWithParameters2Renderer<JetClassOrObject, CallableMemberDescriptor>(
                "{0} must override {1} because it inherits many implementations of it", RENDER_CLASS_OR_OBJECT, DescriptorRenderer.TEXT));

        map.put(CONFLICTING_OVERLOADS,
                new DiagnosticWithParameters2Renderer<CallableMemberDescriptor, String>("{1} is already defined in ''{0}''",
                                                                                        DescriptorRenderer.TEXT, TO_STRING));


        map.put(RESULT_TYPE_MISMATCH,
                new DiagnosticWithParameters3Renderer<String, JetType, JetType>("{0} must return {1} but returns {2}", TO_STRING,
                                                                                RENDER_TYPE, RENDER_TYPE));
        map.put(UNSAFE_INFIX_CALL, new DiagnosticWithParameters3Renderer<String, String, String>(
                "Infix call corresponds to a dot-qualified call ''{0}.{1}({2})'' which is not allowed on a nullable receiver ''{0}''. Use '?.'-qualified call instead",
                TO_STRING, TO_STRING, TO_STRING));

        map.put(OVERLOAD_RESOLUTION_AMBIGUITY, new AmbiguousDescriptorDiagnosticRenderer("Overload resolution ambiguity: {0}"));
        map.put(NONE_APPLICABLE, new AmbiguousDescriptorDiagnosticRenderer(
                "None of the following functions can be called with the arguments supplied: {0}"));
        map.put(NO_VALUE_FOR_PARAMETER, new DiagnosticWithParameters1Renderer<ValueParameterDescriptor>("No value passed for parameter {0}",
                                                                                                        DescriptorRenderer.TEXT));
        map.put(MISSING_RECEIVER, new DiagnosticWithParameters1Renderer<JetType>("A receiver of type {0} is required", RENDER_TYPE));
        map.put(NO_RECEIVER_ADMITTED, new SimpleDiagnosticRenderer("No receiver can be passed to this function or property"));

        map.put(CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS, new SimpleDiagnosticRenderer("Can not create an instance of an abstract class"));
        map.put(TYPE_INFERENCE_FAILED, new DiagnosticWithParameters1Renderer<SolutionStatus>("Type inference failed: {0}", TO_STRING));
        map.put(WRONG_NUMBER_OF_TYPE_ARGUMENTS,
                new DiagnosticWithParameters1Renderer<Integer>("{0} type arguments expected", new Renderer<Integer>() {
                    @NotNull
                    @Override
                    public String render(@NotNull Integer argument) {
                        return argument == 0 ? "No" : argument.toString();
                    }
                }));

        map.put(UNRESOLVED_IDE_TEMPLATE, new DiagnosticWithParameters1Renderer<String>("Unresolved IDE template: {0}", TO_STRING));

        map.put(DANGLING_FUNCTION_LITERAL_ARGUMENT_SUSPECTED, new SimpleDiagnosticRenderer(
                "This expression is treated as an argument to the function call on the previous line. Separate it with a semicolon (;) if it is not intended to be an argument."));

        map.put(NOT_AN_ANNOTATION_CLASS, new DiagnosticWithParameters1Renderer<String>("{0} is not an annotation class", TO_STRING));
    }

    @NotNull
    @Override
    public String render(@NotNull Diagnostic diagnostic) {
        DiagnosticRenderer renderer = map.get(diagnostic.getFactory());
        if (renderer == null) {
            throw new IllegalArgumentException("Don't know how to render diagnostic of type " + diagnostic.getFactory().getName());
        }
        //noinspection unchecked
        return renderer.render(diagnostic);
    }

    private static class RedeclarationDiagnosticRenderer implements DiagnosticRenderer<RedeclarationDiagnostic> {
        private String messagePrefix;

        private RedeclarationDiagnosticRenderer(@NotNull String messagePrefix) {
            this.messagePrefix = messagePrefix;
        }

        @NotNull
        @Override
        public String render(@NotNull RedeclarationDiagnostic diagnostic) {
            return messagePrefix + diagnostic.getName();
        }
    }

    private static class UnresolvedReferenceDiagnosticRenderer implements DiagnosticRenderer<UnresolvedReferenceDiagnostic> {
        private String messagePrefix;

        private UnresolvedReferenceDiagnosticRenderer(@NotNull String messagePrefix) {
            this.messagePrefix = messagePrefix;
        }

        @NotNull
        @Override
        public String render(@NotNull UnresolvedReferenceDiagnostic diagnostic) {
            return messagePrefix + diagnostic.getPsiElement().getText();
        }
    }

    private static class AmbiguousDescriptorDiagnosticRenderer
            extends DiagnosticWithParameters1Renderer<Collection<? extends ResolvedCall<? extends CallableDescriptor>>> {
        private AmbiguousDescriptorDiagnosticRenderer(@NotNull String message) {
            super(message, new Renderer<Collection<? extends ResolvedCall<? extends CallableDescriptor>>>() {
                @NotNull
                @Override
                public String render(@NotNull Collection<? extends ResolvedCall<? extends CallableDescriptor>> argument) {
                    StringBuilder stringBuilder = new StringBuilder("\n");
                    for (ResolvedCall<? extends CallableDescriptor> call : argument) {
                        stringBuilder.append(DescriptorRenderer.TEXT.render(call.getResultingDescriptor())).append("\n");
                    }
                    return stringBuilder.toString();
                }
            });
        }
    }
}
