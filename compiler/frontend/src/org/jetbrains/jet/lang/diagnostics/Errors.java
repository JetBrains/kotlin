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

package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.inference.SolutionStatus;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Severity.ERROR;
import static org.jetbrains.jet.lang.diagnostics.Severity.WARNING;

import static org.jetbrains.jet.lang.diagnostics.Renderers.*;

/**
 * @author abreslav
 */
public interface Errors {

    DiagnosticFactory1<JetFile, Throwable> EXCEPTION_WHILE_ANALYZING = DiagnosticFactory1.create(ERROR, "{0}", new Renderer<Throwable>() {
        @NotNull
        @Override
        public String render(@Nullable Throwable e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    });

    UnresolvedReferenceDiagnosticFactory UNRESOLVED_REFERENCE = UnresolvedReferenceDiagnosticFactory.create("Unresolved reference");

    RedeclarationDiagnosticFactory REDECLARATION = RedeclarationDiagnosticFactory.REDECLARATION;
    RedeclarationDiagnosticFactory NAME_SHADOWING = RedeclarationDiagnosticFactory.NAME_SHADOWING;

    DiagnosticFactory2<PsiElement, JetType, JetType> TYPE_MISMATCH = DiagnosticFactory2.create(ERROR, "Type mismatch: inferred type is {1} but {0} was expected");
    DiagnosticFactory1<PsiElement, Collection<JetKeywordToken>> INCOMPATIBLE_MODIFIERS =
            DiagnosticFactory1.create(ERROR, "Incompatible modifiers: ''{0}''",
                                      new Renderer<Collection<JetKeywordToken>>() {
                                          @NotNull
                                          @Override
                                          public String render(@Nullable Collection<JetKeywordToken> element) {
                                              assert element != null;
                                              StringBuilder sb = new StringBuilder();
                                              for (Iterator<JetKeywordToken> iterator = element.iterator(); iterator.hasNext(); ) {
                                                  JetKeywordToken modifier = iterator.next();
                                                  sb.append(modifier.getValue());
                                                  if (iterator.hasNext()) {
                                                      sb.append(" ");
                                                  }
                                              }
                                              return sb.toString();
                                          }
                                      });
    DiagnosticFactory1<PsiElement, JetKeywordToken> ILLEGAL_MODIFIER = DiagnosticFactory1.create(ERROR, "Illegal modifier ''{0}''");

    DiagnosticFactory2<PsiElement, JetKeywordToken, JetKeywordToken> REDUNDANT_MODIFIER = DiagnosticFactory2.create(Severity.WARNING, "Modifier {0} is redundant because {1} is present");
    DiagnosticFactory<JetModifierListOwner> ABSTRACT_MODIFIER_IN_TRAIT = DiagnosticFactory.create(WARNING, "Modifier ''{0}'' is redundant in trait", PositioningStrategies.POSITION_ABSTRACT_MODIFIER);
    DiagnosticFactory<JetModifierListOwner> OPEN_MODIFIER_IN_TRAIT = DiagnosticFactory.create(WARNING, "Modifier ''{0}'' is redundant in trait", PositioningStrategies.positionModifier(JetTokens.OPEN_KEYWORD));
    DiagnosticFactory<PsiElement> REDUNDANT_MODIFIER_IN_GETTER = DiagnosticFactory.create(WARNING, "Visibility modifiers are redundant in getter");
    DiagnosticFactory<PsiElement> TRAIT_CAN_NOT_BE_FINAL = DiagnosticFactory.create(ERROR, "Trait can not be final");
    DiagnosticFactory<JetExpression> TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM = DiagnosticFactory.create(ERROR, "Type checking has run into a recursive problem. Easiest workaround: specify types of your declarations explicitly"); // TODO: message
    DiagnosticFactory<JetReturnExpression> RETURN_NOT_ALLOWED = DiagnosticFactory.create(ERROR, "'return' is not allowed here");
    DiagnosticFactory<JetTypeProjection> PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE = DiagnosticFactory.create(ERROR, "Projections are not allowed for immediate arguments of a supertype", new PositioningStrategy<JetTypeProjection>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetTypeProjection element) {
            return markNode(element.getProjectionNode());
        }
    });
    DiagnosticFactory<JetSimpleNameExpression> LABEL_NAME_CLASH = DiagnosticFactory.create(WARNING, "There is more than one label with such a name in this scope");
    DiagnosticFactory<JetSimpleNameExpression> EXPRESSION_EXPECTED_NAMESPACE_FOUND = DiagnosticFactory.create(ERROR, "Expression expected, but a namespace name found");

    DiagnosticFactory1<JetSimpleNameExpression, DeclarationDescriptor> CANNOT_IMPORT_FROM_ELEMENT = DiagnosticFactory1.create(ERROR, "Cannot import from ''{0}''", NAME);
    DiagnosticFactory1<JetSimpleNameExpression, DeclarationDescriptor> CANNOT_BE_IMPORTED = DiagnosticFactory1.create(ERROR, "Cannot import ''{0}'', functions and properties can be imported only from packages", NAME);
    DiagnosticFactory<JetExpression> USELESS_HIDDEN_IMPORT = DiagnosticFactory.create(WARNING, "Useless import, it is hidden further");
    DiagnosticFactory<JetExpression> USELESS_SIMPLE_IMPORT = DiagnosticFactory.create(WARNING, "Useless import, does nothing");

    DiagnosticFactory<JetParameter> CANNOT_INFER_PARAMETER_TYPE = DiagnosticFactory.create(ERROR, "Cannot infer a type for this parameter. To specify it explicitly use the {(p : Type) => ...} notation");

    DiagnosticFactory<JetElement> NO_BACKING_FIELD_ABSTRACT_PROPERTY = DiagnosticFactory.create(ERROR, "This property doesn't have a backing field, because it's abstract");
    DiagnosticFactory<JetElement> NO_BACKING_FIELD_CUSTOM_ACCESSORS = DiagnosticFactory.create(ERROR, "This property doesn't have a backing field, because it has custom accessors without reference to the backing field");
    DiagnosticFactory<JetElement> INACCESSIBLE_BACKING_FIELD = DiagnosticFactory.create(ERROR, "The backing field is not accessible here");
    DiagnosticFactory<JetElement> NOT_PROPERTY_BACKING_FIELD = DiagnosticFactory.create(ERROR, "The referenced variable is not a property and doesn't have backing field");

    DiagnosticFactory<PsiElement> MIXING_NAMED_AND_POSITIONED_ARGUMENTS = DiagnosticFactory.create(ERROR, "Mixing named and positioned arguments in not allowed");
    DiagnosticFactory<JetReferenceExpression> ARGUMENT_PASSED_TWICE = DiagnosticFactory.create(ERROR, "An argument is already passed for this parameter");
    UnresolvedReferenceDiagnosticFactory NAMED_PARAMETER_NOT_FOUND = UnresolvedReferenceDiagnosticFactory.create("Cannot find a parameter with this name");
    DiagnosticFactory<JetExpression> VARARG_OUTSIDE_PARENTHESES = DiagnosticFactory.create(ERROR, "Passing value as a vararg is only allowed inside a parenthesized argument list");

    DiagnosticFactory<JetExpression> MANY_FUNCTION_LITERAL_ARGUMENTS = DiagnosticFactory.create(ERROR, "Only one function literal is allowed outside a parenthesized argument list");
    DiagnosticFactory<PsiElement> PROPERTY_WITH_NO_TYPE_NO_INITIALIZER = DiagnosticFactory.create(ERROR, "This property must either have a type annotation or be initialized");

    DiagnosticFactory<JetModifierListOwner> ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS = DiagnosticFactory.create(ERROR, "This property cannot be declared abstract", PositioningStrategies.POSITION_ABSTRACT_MODIFIER);
    DiagnosticFactory<JetProperty> ABSTRACT_PROPERTY_NOT_IN_CLASS = DiagnosticFactory.create(ERROR, "A property may be abstract only when defined in a class or trait", PositioningStrategies.POSITION_ABSTRACT_MODIFIER);
    DiagnosticFactory<JetExpression> ABSTRACT_PROPERTY_WITH_INITIALIZER = DiagnosticFactory.create(ERROR, "Property with initializer cannot be abstract");
    DiagnosticFactory<JetPropertyAccessor> ABSTRACT_PROPERTY_WITH_GETTER = DiagnosticFactory.create(ERROR, "Property with getter implementation cannot be abstract");
    DiagnosticFactory<JetPropertyAccessor> ABSTRACT_PROPERTY_WITH_SETTER = DiagnosticFactory.create(ERROR, "Property with setter implementation cannot be abstract");

    DiagnosticFactory<PsiElement> GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY = DiagnosticFactory.create(ERROR, "Getter visibility must be the same as property visibility");
    DiagnosticFactory<JetProperty> BACKING_FIELD_IN_TRAIT = DiagnosticFactory.create(ERROR, "Property in a trait cannot have a backing field", PositioningStrategies.POSITION_NAME_IDENTIFIER);
    DiagnosticFactory<JetProperty> MUST_BE_INITIALIZED = DiagnosticFactory.create(ERROR, "Property must be initialized", PositioningStrategies.POSITION_NAME_IDENTIFIER);
    DiagnosticFactory<JetProperty> MUST_BE_INITIALIZED_OR_BE_ABSTRACT = DiagnosticFactory.create(ERROR, "Property must be initialized or be abstract", PositioningStrategies.POSITION_NAME_IDENTIFIER);
    DiagnosticFactory<JetExpression> PROPERTY_INITIALIZER_IN_TRAIT = DiagnosticFactory.create(ERROR, "Property initializers are not allowed in traits");
    DiagnosticFactory<JetExpression> PROPERTY_INITIALIZER_NO_BACKING_FIELD = DiagnosticFactory.create(ERROR, "Initializer is not allowed here because this property has no backing field");
    DiagnosticFactory3<JetModifierListOwner, String, ClassDescriptor, JetClass> ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS = DiagnosticFactory3.create(ERROR, "Abstract property {0} in non-abstract class {1}", PositioningStrategies.POSITION_ABSTRACT_MODIFIER);
    DiagnosticFactory3<JetFunction, String, ClassDescriptor, JetClass> ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS = DiagnosticFactory3.create(ERROR, "Abstract function {0} in non-abstract class {1}", PositioningStrategies.POSITION_ABSTRACT_MODIFIER);
    DiagnosticFactory1<JetFunction, SimpleFunctionDescriptor> ABSTRACT_FUNCTION_WITH_BODY = DiagnosticFactory1.create(ERROR, "A function {0} with body cannot be abstract", PositioningStrategies.POSITION_ABSTRACT_MODIFIER);
    DiagnosticFactory1<JetFunction, SimpleFunctionDescriptor> NON_ABSTRACT_FUNCTION_WITH_NO_BODY = DiagnosticFactory1.create(ERROR, "Method {0} without a body must be abstract", PositioningStrategies.POSITION_NAME_IDENTIFIER);
    DiagnosticFactory1<JetModifierListOwner, SimpleFunctionDescriptor> NON_MEMBER_ABSTRACT_FUNCTION = DiagnosticFactory1.create(ERROR, "Function {0} is not a class or trait member and cannot be abstract", PositioningStrategies.POSITION_ABSTRACT_MODIFIER);

    DiagnosticFactory1<JetFunction, SimpleFunctionDescriptor> NON_MEMBER_FUNCTION_NO_BODY = DiagnosticFactory1.create(ERROR, "Function {0} must have a body", PositioningStrategies.POSITION_NAME_IDENTIFIER);
    DiagnosticFactory<JetNamedDeclaration> NON_FINAL_MEMBER_IN_FINAL_CLASS = DiagnosticFactory.create(ERROR, "Non final member in a final class", PositioningStrategies.positionModifier(JetTokens.OPEN_KEYWORD));

    DiagnosticFactory<JetNamedDeclaration> PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE = DiagnosticFactory.create(ERROR, "Public or protected member should specify a type", PositioningStrategies.POSITION_NAME_IDENTIFIER);

    DiagnosticFactory<JetTypeProjection> PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT = DiagnosticFactory.create(ERROR, "Projections are not allowed on type arguments of functions and properties"); // TODO : better positioning
    DiagnosticFactory<JetDelegatorToSuperClass> SUPERTYPE_NOT_INITIALIZED = DiagnosticFactory.create(ERROR, "This type has a constructor, and thus must be initialized here");
    DiagnosticFactory<JetDelegatorToSuperClass> SUPERTYPE_NOT_INITIALIZED_DEFAULT = DiagnosticFactory.create(ERROR, "Constructor invocation should be explicitly specified");
    DiagnosticFactory<PsiElement> SECONDARY_CONSTRUCTOR_BUT_NO_PRIMARY = DiagnosticFactory.create(ERROR, "A secondary constructor may appear only in a class that has a primary constructor");
    DiagnosticFactory<PsiElement> SECONDARY_CONSTRUCTOR_NO_INITIALIZER_LIST = DiagnosticFactory.create(ERROR, "Secondary constructors must have an initializer list");
    DiagnosticFactory<JetDelegatorByExpressionSpecifier> BY_IN_SECONDARY_CONSTRUCTOR = DiagnosticFactory.create(ERROR, "'by'-clause is only supported for primary constructors");
    DiagnosticFactory<JetDelegatorToSuperClass> INITIALIZER_WITH_NO_ARGUMENTS = DiagnosticFactory.create(ERROR, "Constructor arguments required");
    DiagnosticFactory<JetDelegationSpecifier> MANY_CALLS_TO_THIS = DiagnosticFactory.create(ERROR, "Only one call to 'this(...)' is allowed");
    DiagnosticFactory1<JetModifierListOwner, CallableMemberDescriptor> NOTHING_TO_OVERRIDE = DiagnosticFactory1.create(ERROR, "{0} overrides nothing", PositioningStrategies.positionModifier(JetTokens.OVERRIDE_KEYWORD), DescriptorRenderer.TEXT);
    DiagnosticFactory3<PsiNameIdentifierOwner, CallableMemberDescriptor, CallableMemberDescriptor, DeclarationDescriptor> VIRTUAL_MEMBER_HIDDEN = DiagnosticFactory3.create(ERROR, "''{0}'' hides ''{1}'' in class {2} and needs 'override' modifier", PositioningStrategies.POSITION_NAME_IDENTIFIER, DescriptorRenderer.TEXT, DescriptorRenderer.TEXT, DescriptorRenderer.TEXT);

    DiagnosticFactory1<JetClass, ClassDescriptor> ENUM_ENTRY_SHOULD_BE_INITIALIZED = DiagnosticFactory1.create(ERROR, "Missing delegation specifier ''{0}''", PositioningStrategies.POSITION_NAME_IDENTIFIER, NAME);
    DiagnosticFactory1<JetTypeReference, ClassDescriptor> ENUM_ENTRY_ILLEGAL_TYPE = DiagnosticFactory1.create(ERROR, "The type constructor of enum entry should be ''{0}''", NAME);

    DiagnosticFactory1<JetSimpleNameExpression, VariableDescriptor> UNINITIALIZED_VARIABLE = DiagnosticFactory1.create(ERROR, "Variable ''{0}'' must be initialized", NAME);
    DiagnosticFactory1<JetSimpleNameExpression, ValueParameterDescriptor> UNINITIALIZED_PARAMETER = DiagnosticFactory1.create(ERROR, "Parameter ''{0}'' is uninitialized here", NAME);
    UnusedElementDiagnosticFactory<JetProperty, VariableDescriptor> UNUSED_VARIABLE = UnusedElementDiagnosticFactory.create(WARNING, "Variable ''{0}'' is never used", PositioningStrategies.POSITION_NAME_IDENTIFIER, NAME);
    UnusedElementDiagnosticFactory<JetParameter, VariableDescriptor> UNUSED_PARAMETER = UnusedElementDiagnosticFactory.create(WARNING, "Parameter ''{0}'' is never used", PositioningStrategies.POSITION_NAME_IDENTIFIER, NAME);
    UnusedElementDiagnosticFactory<JetNamedDeclaration, DeclarationDescriptor> ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE = UnusedElementDiagnosticFactory.create(WARNING, "Variable ''{0}'' is assigned but never accessed", PositioningStrategies.POSITION_NAME_IDENTIFIER, NAME);
    DiagnosticFactory1<JetExpression, DeclarationDescriptor> VARIABLE_WITH_REDUNDANT_INITIALIZER = DiagnosticFactory1.create(WARNING, "Variable ''{0}'' initializer is redundant", NAME);
    DiagnosticFactory2<JetElement, JetElement, DeclarationDescriptor> UNUSED_VALUE = DiagnosticFactory2.create(WARNING, "The value ''{0}'' assigned to ''{1}'' is never used", ELEMENT_TEXT, TO_STRING);
    DiagnosticFactory1<JetElement, JetElement> UNUSED_CHANGED_VALUE = DiagnosticFactory1.create(WARNING, "The value changed at ''{0}'' is never used", ELEMENT_TEXT);
    DiagnosticFactory<JetElement> UNUSED_EXPRESSION = DiagnosticFactory.create(WARNING, "The expression is unused");
    DiagnosticFactory<JetFunctionLiteralExpression> UNUSED_FUNCTION_LITERAL = DiagnosticFactory.create(WARNING, "The function literal is unused. If you mean block, you can use 'run { ... }'");

    DiagnosticFactory1<JetExpression, DeclarationDescriptor> VAL_REASSIGNMENT = DiagnosticFactory1.create(ERROR, "Val can not be reassigned", NAME);
    DiagnosticFactory1<JetExpression, DeclarationDescriptor> INITIALIZATION_BEFORE_DECLARATION = DiagnosticFactory1.create(ERROR, "Variable cannot be initialized before declaration", NAME);
    DiagnosticFactory<JetExpression> VARIABLE_EXPECTED = DiagnosticFactory.create(ERROR, "Variable expected");

    DiagnosticFactory1<JetExpression, DeclarationDescriptor> INITIALIZATION_USING_BACKING_FIELD_CUSTOM_SETTER = DiagnosticFactory1.create(ERROR, "This property has a custom setter, so initialization using backing field required", NAME);
    DiagnosticFactory1<JetExpression, DeclarationDescriptor> INITIALIZATION_USING_BACKING_FIELD_OPEN_SETTER = DiagnosticFactory1.create(ERROR, "Setter of this property can be overridden, so initialization using backing field required", NAME);

    DiagnosticFactory1<JetSimpleNameExpression, DeclarationDescriptor> FUNCTION_PARAMETERS_OF_INLINE_FUNCTION = DiagnosticFactory1.create(ERROR, "Function parameters of inline function can only be invoked", NAME);

    DiagnosticFactory<JetElement> UNREACHABLE_CODE = DiagnosticFactory.create(ERROR, "Unreachable code");

    DiagnosticFactory<JetClassObject> MANY_CLASS_OBJECTS = DiagnosticFactory.create(ERROR, "Only one class object is allowed per class");
    DiagnosticFactory<JetClassObject> CLASS_OBJECT_NOT_ALLOWED = DiagnosticFactory.create(ERROR, "A class object is not allowed here");
    DiagnosticFactory<JetDelegatorByExpressionSpecifier> DELEGATION_IN_TRAIT = DiagnosticFactory.create(ERROR, "Traits cannot use delegation");
    DiagnosticFactory<JetTypeReference> DELEGATION_NOT_TO_TRAIT = DiagnosticFactory.create(ERROR, "Only traits can be delegated to");
    DiagnosticFactory<PsiElement> NO_CONSTRUCTOR = DiagnosticFactory.create(ERROR, "This class does not have a constructor");
    DiagnosticFactory<JetExpression> NOT_A_CLASS = DiagnosticFactory.create(ERROR, "Not a class");
    DiagnosticFactory<JetEscapeStringTemplateEntry> ILLEGAL_ESCAPE_SEQUENCE = DiagnosticFactory.create(ERROR, "Illegal escape sequence");

    DiagnosticFactory<JetTypeReference> LOCAL_EXTENSION_PROPERTY = DiagnosticFactory.create(ERROR, "Local extension properties are not allowed");
    DiagnosticFactory<JetPropertyAccessor> LOCAL_VARIABLE_WITH_GETTER = DiagnosticFactory.create(ERROR, "Local variables are not allowed to have getters");
    DiagnosticFactory<JetPropertyAccessor> LOCAL_VARIABLE_WITH_SETTER = DiagnosticFactory.create(ERROR, "Local variables are not allowed to have setters");
    DiagnosticFactory<JetPropertyAccessor> VAL_WITH_SETTER = DiagnosticFactory.create(ERROR, "A 'val'-property cannot have a setter");

    DiagnosticFactory<JetArrayAccessExpression> NO_GET_METHOD = DiagnosticFactory.create(ERROR, "No get method providing array access", PositioningStrategies.POSITION_ARRAY_ACCESS);
    DiagnosticFactory<JetArrayAccessExpression> NO_SET_METHOD = DiagnosticFactory.create(ERROR, "No set method providing array access", PositioningStrategies.POSITION_ARRAY_ACCESS);

    DiagnosticFactory<JetSimpleNameExpression> INC_DEC_SHOULD_NOT_RETURN_UNIT = DiagnosticFactory.create(ERROR, "Functions inc(), dec() shouldn't return Unit to be used by operators ++, --");
    DiagnosticFactory2<JetSimpleNameExpression, DeclarationDescriptor, JetSimpleNameExpression> ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT =
            DiagnosticFactory2.create(ERROR, "Function ''{0}'' should return Unit to be used by corresponding operator ''{1}''", NAME, ELEMENT_TEXT);
    AmbiguousDescriptorDiagnosticFactory ASSIGN_OPERATOR_AMBIGUITY = AmbiguousDescriptorDiagnosticFactory.create("Assignment operators ambiguity: {0}");

    DiagnosticFactory<JetSimpleNameExpression> EQUALS_MISSING = DiagnosticFactory.create(ERROR, "No method 'equals(Any?) : Boolean' available");
    DiagnosticFactory<JetBinaryExpression> ASSIGNMENT_IN_EXPRESSION_CONTEXT = DiagnosticFactory.create(ERROR, "Assignments are not expressions, and only expressions are allowed in this context");
    DiagnosticFactory<JetRootNamespaceExpression> NAMESPACE_IS_NOT_AN_EXPRESSION = DiagnosticFactory.create(ERROR, "'namespace' is not an expression, it can only be used on the left-hand side of a dot ('.')");
    DiagnosticFactory1<JetSuperExpression, String> SUPER_IS_NOT_AN_EXPRESSION = DiagnosticFactory1.create(ERROR, "{0} is not an expression, it can only be used on the left-hand side of a dot ('.')");
    DiagnosticFactory<JetDeclaration> DECLARATION_IN_ILLEGAL_CONTEXT = DiagnosticFactory.create(ERROR, "Declarations are not allowed in this position");
    DiagnosticFactory<JetExpression> SETTER_PARAMETER_WITH_DEFAULT_VALUE = DiagnosticFactory.create(ERROR, "Setter parameters can not have default values");
    DiagnosticFactory<JetThisExpression> NO_THIS = DiagnosticFactory.create(ERROR, "'this' is not defined in this context");
    DiagnosticFactory<JetSuperExpression> SUPER_NOT_AVAILABLE = DiagnosticFactory.create(ERROR, "No supertypes are accessible in this context");
    DiagnosticFactory<JetSuperExpression> AMBIGUOUS_SUPER = DiagnosticFactory.create(ERROR, "Many supertypes available, please specify the one you mean in angle brackets, e.g. 'super<Foo>'");
    DiagnosticFactory<JetExpression> ABSTRACT_SUPER_CALL = DiagnosticFactory.create(ERROR, "Abstract member cannot be accessed directly");
    DiagnosticFactory<JetTypeReference> NOT_A_SUPERTYPE = DiagnosticFactory.create(ERROR, "Not a supertype");
    DiagnosticFactory<PsiElement> TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER = DiagnosticFactory.create(WARNING, "Type arguments do not need to be specified in a 'super' qualifier");
    DiagnosticFactory<JetSimpleNameExpression> USELESS_CAST_STATIC_ASSERT_IS_FINE = DiagnosticFactory.create(WARNING, "No cast needed, use ':' instead");
    DiagnosticFactory<JetSimpleNameExpression> USELESS_CAST = DiagnosticFactory.create(WARNING, "No cast needed");
    DiagnosticFactory<JetSimpleNameExpression> CAST_NEVER_SUCCEEDS = DiagnosticFactory.create(WARNING, "This cast can never succeed");
    DiagnosticFactory1<JetTypeReference, JetType> WRONG_SETTER_PARAMETER_TYPE = DiagnosticFactory1.create(ERROR, "Setter parameter type must be equal to the type of the property, i.e. {0}");//, DiagnosticParameters.TYPE);
    DiagnosticFactory1<JetTypeReference, JetType> WRONG_GETTER_RETURN_TYPE = DiagnosticFactory1.create(ERROR, "Getter return type must be equal to the type of the property, i.e. {0}");//, DiagnosticParameters.TYPE);
    DiagnosticFactory1<JetSimpleNameExpression, ClassifierDescriptor> NO_CLASS_OBJECT = DiagnosticFactory1.create(ERROR, "Please specify constructor invocation; classifier {0} does not have a class object", NAME);
    DiagnosticFactory<PsiElement> NO_GENERICS_IN_SUPERTYPE_SPECIFIER = DiagnosticFactory.create(ERROR, "Generic arguments of the base type must be specified");

    DiagnosticFactory<JetExpression> HAS_NEXT_PROPERTY_AND_FUNCTION_AMBIGUITY = DiagnosticFactory.create(ERROR, "An ambiguity between 'iterator().hasNext()' function and 'iterator().hasNext' property");
    DiagnosticFactory<JetExpression> HAS_NEXT_MISSING = DiagnosticFactory.create(ERROR, "Loop range must have an 'iterator().hasNext()' function or an 'iterator().hasNext' property");
    DiagnosticFactory<JetExpression> HAS_NEXT_FUNCTION_AMBIGUITY = DiagnosticFactory.create(ERROR, "Function 'iterator().hasNext()' is ambiguous for this expression");
    DiagnosticFactory<JetExpression> HAS_NEXT_MUST_BE_READABLE = DiagnosticFactory.create(ERROR, "The 'iterator().hasNext' property of the loop range must be readable");
    DiagnosticFactory1<JetExpression, JetType> HAS_NEXT_PROPERTY_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR, "The 'iterator().hasNext' property of the loop range must return Boolean, but returns {0}");
    DiagnosticFactory1<JetExpression, JetType> HAS_NEXT_FUNCTION_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR, "The 'iterator().hasNext()' function of the loop range must return Boolean, but returns {0}");
    DiagnosticFactory<JetExpression> NEXT_AMBIGUITY = DiagnosticFactory.create(ERROR, "Function 'iterator().next()' is ambiguous for this expression");
    DiagnosticFactory<JetExpression> NEXT_MISSING = DiagnosticFactory.create(ERROR, "Loop range must have an 'iterator().next()' function");
    DiagnosticFactory<JetExpression> ITERATOR_MISSING = DiagnosticFactory.create(ERROR, "For-loop range must have an iterator() method");
    AmbiguousDescriptorDiagnosticFactory ITERATOR_AMBIGUITY = AmbiguousDescriptorDiagnosticFactory.create("Method 'iterator()' is ambiguous for this expression: {0}");

    DiagnosticFactory1<JetSimpleNameExpression, JetType> COMPARE_TO_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR, "compareTo() must return Int, but returns {0}");
    DiagnosticFactory1<JetExpression, JetType> CALLEE_NOT_A_FUNCTION = DiagnosticFactory1.create(ERROR, "Expecting a function type, but found {0}");

    DiagnosticFactory<JetReturnExpression> RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY = DiagnosticFactory.create(ERROR, "Returns are not allowed for functions with expression body. Use block body in '{...}'");
    DiagnosticFactory<JetDeclarationWithBody> NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY = DiagnosticFactory.create(ERROR, "A 'return' expression required in a function with a block body ('{...}')", new PositioningStrategy<JetDeclarationWithBody>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetDeclarationWithBody element) {
            JetExpression bodyExpression = element.getBodyExpression();
            if (!(bodyExpression instanceof JetBlockExpression)) return Collections.emptyList();
            JetBlockExpression blockExpression = (JetBlockExpression) bodyExpression;
            TextRange lastBracketRange = blockExpression.getLastBracketRange();
            if (lastBracketRange == null) return Collections.emptyList();
            return markRange(lastBracketRange);
        }
    });
    DiagnosticFactory1<JetExpression, JetType> RETURN_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR, "This function must return a value of type {0}");
    DiagnosticFactory1<JetExpression, JetType> EXPECTED_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR, "Expected a value of type {0}");
    DiagnosticFactory1<JetBinaryExpression, JetType> ASSIGNMENT_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR, "Expected a value of type {0}. Assignment operation is not an expression, so it does not return any value");
    DiagnosticFactory1<JetExpression, JetType> IMPLICIT_CAST_TO_UNIT_OR_ANY = DiagnosticFactory1.create(WARNING, "Type was casted to ''{0}''. Please specify ''{0}'' as expected type, if you mean such cast");
    DiagnosticFactory1<JetExpression, JetExpression> EXPRESSION_EXPECTED = DiagnosticFactory1.create(ERROR, "{0} is not an expression, and only expression are allowed here", new Renderer<JetExpression>() {
        @NotNull
        @Override
        public String render(@Nullable JetExpression expression) {
            assert expression != null;
            String expressionType = expression.toString();
            return expressionType.substring(0, 1) + expressionType.substring(1).toLowerCase();
        }
    });

    DiagnosticFactory1<JetTypeReference, JetType> UPPER_BOUND_VIOLATED = DiagnosticFactory1.create(ERROR, "An upper bound {0} is violated"); // TODO : Message
    DiagnosticFactory1<JetTypeReference, JetType> FINAL_CLASS_OBJECT_UPPER_BOUND = DiagnosticFactory1.create(ERROR, "{0} is a final type, and thus a class object cannot extend it");
    DiagnosticFactory1<JetTypeReference, JetType> FINAL_UPPER_BOUND = DiagnosticFactory1.create(WARNING, "{0} is a final type, and thus a value of the type parameter is predetermined");
    DiagnosticFactory1<JetExpression, JetType> USELESS_ELVIS = DiagnosticFactory1.create(WARNING, "Elvis operator (?:) always returns the left operand of non-nullable type {0}");
    DiagnosticFactory1<PsiElement, TypeParameterDescriptor> CONFLICTING_UPPER_BOUNDS = DiagnosticFactory1.create(ERROR, "Upper bounds of {0} have empty intersection", NAME);
    DiagnosticFactory1<PsiElement, TypeParameterDescriptor> CONFLICTING_CLASS_OBJECT_UPPER_BOUNDS = DiagnosticFactory1.create(ERROR, "Class object upper bounds of {0} have empty intersection", NAME);

    DiagnosticFactory1<PsiElement, CallableDescriptor> TOO_MANY_ARGUMENTS = DiagnosticFactory1.create(ERROR, "Too many arguments for {0}");
    DiagnosticFactory1<PsiElement, String> ERROR_COMPILE_TIME_VALUE = DiagnosticFactory1.create(ERROR, "{0}");

    DiagnosticFactory<JetWhenEntry> ELSE_MISPLACED_IN_WHEN = DiagnosticFactory.create(
            ERROR, "'else' entry must be the last one in a when-expression", new PositioningStrategy<JetWhenEntry>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetWhenEntry entry) {
            PsiElement elseKeywordElement = entry.getElseKeywordElement();
            assert elseKeywordElement != null;
            return markElement(elseKeywordElement);
        }
    });

    DiagnosticFactory<JetWhenExpression> NO_ELSE_IN_WHEN = new DiagnosticFactory<JetWhenExpression>(ERROR, "'when' expression must contain 'else' branch", new PositioningStrategy<JetWhenExpression>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetWhenExpression element) {
            if (hasSyntaxError(element)) return Collections.emptyList();
            return markElement(element.getWhenKeywordElement());
        }
    });
    DiagnosticFactory<JetWhenConditionInRange> TYPE_MISMATCH_IN_RANGE = new DiagnosticFactory<JetWhenConditionInRange>(ERROR, "Type mismatch: incompatible types of range and element checked in it", new PositioningStrategy<JetWhenConditionInRange>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetWhenConditionInRange condition) {
            return markElement(condition.getOperationReference());
        }
    });
    DiagnosticFactory<PsiElement> CYCLIC_INHERITANCE_HIERARCHY = DiagnosticFactory.create(ERROR, "There's a cycle in the inheritance hierarchy for this type");

    DiagnosticFactory<JetTypeReference> MANY_CLASSES_IN_SUPERTYPE_LIST = DiagnosticFactory.create(ERROR, "Only one class may appear in a supertype list");
    DiagnosticFactory<JetTypeReference> SUPERTYPE_NOT_A_CLASS_OR_TRAIT = DiagnosticFactory.create(ERROR, "Only classes and traits may serve as supertypes");
    DiagnosticFactory<PsiElement> SUPERTYPE_INITIALIZED_IN_TRAIT = DiagnosticFactory.create(ERROR, "Traits cannot initialize supertypes");
    DiagnosticFactory<PsiElement> CONSTRUCTOR_IN_TRAIT = DiagnosticFactory.create(ERROR, "A trait may not have a constructor");
    DiagnosticFactory<JetTypeReference> SUPERTYPE_APPEARS_TWICE = DiagnosticFactory.create(ERROR, "A supertype appears twice");
    DiagnosticFactory<JetTypeReference> FINAL_SUPERTYPE = DiagnosticFactory.create(ERROR, "This type is final, so it cannot be inherited from");

    DiagnosticFactory1<JetExpression, String> ILLEGAL_SELECTOR = DiagnosticFactory1.create(ERROR, "Expression ''{0}'' cannot be a selector (occur after a dot)");

    DiagnosticFactory<JetParameter> VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION = DiagnosticFactory.create(ERROR, "A type annotation is required on a value parameter");
    DiagnosticFactory<JetLabelQualifiedExpression> BREAK_OR_CONTINUE_OUTSIDE_A_LOOP = DiagnosticFactory.create(ERROR, "'break' and 'continue' are only allowed inside a loop");
    DiagnosticFactory1<JetLabelQualifiedExpression, String> NOT_A_LOOP_LABEL = DiagnosticFactory1.create(ERROR, "The label ''{0}'' does not denote a loop");
    DiagnosticFactory1<JetReturnExpression, String> NOT_A_RETURN_LABEL = DiagnosticFactory1.create(ERROR, "The label ''{0}'' does not reference to a context from which we can return");

    DiagnosticFactory<JetClassInitializer> ANONYMOUS_INITIALIZER_WITHOUT_CONSTRUCTOR = DiagnosticFactory.create(ERROR, "Anonymous initializers are only allowed in the presence of a primary constructor");
    DiagnosticFactory<JetNullableType> NULLABLE_SUPERTYPE = DiagnosticFactory.create(ERROR, "A supertype cannot be nullable", new PositioningStrategy<JetNullableType>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetNullableType element) {
            return markNode(element.getQuestionMarkNode());
        }
    });
    DiagnosticFactory1<PsiElement, JetType> UNSAFE_CALL = DiagnosticFactory1.create(ERROR, "Only safe calls (?.) are allowed on a nullable receiver of type {0}");
    DiagnosticFactory<JetSimpleNameExpression> AMBIGUOUS_LABEL = DiagnosticFactory.create(ERROR, "Ambiguous label");
    DiagnosticFactory1<PsiElement, String> UNSUPPORTED = DiagnosticFactory1.create(ERROR, "Unsupported [{0}]");
    DiagnosticFactory1<PsiElement, JetType> UNNECESSARY_SAFE_CALL = DiagnosticFactory1.create(WARNING, "Unnecessary safe call on a non-null receiver of type {0}");
    DiagnosticFactory1<PsiElement, JetType> UNNECESSARY_NOT_NULL_ASSERTION = DiagnosticFactory1.create(WARNING, "Unnecessary non-null assertion (!!) on a non-null receiver of type {0}");
    DiagnosticFactory2<JetSimpleNameExpression, JetTypeConstraint, JetTypeParameterListOwner> NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER = DiagnosticFactory2.create(ERROR, "{0} does not refer to a type parameter of {1}", new Renderer<JetTypeConstraint>() {
        @NotNull
        @Override
        public String render(@Nullable JetTypeConstraint typeConstraint) {
            assert typeConstraint != null;
            return typeConstraint.getSubjectTypeParameterName().getReferencedName();
        }
    }, NAME);
    DiagnosticFactory2<JetExpression, JetType, String> AUTOCAST_IMPOSSIBLE = DiagnosticFactory2.create(ERROR, "Automatic cast to {0} is impossible, because {1} could have changed since the is-check");

    DiagnosticFactory2<JetTypeReference, JetType, JetType> TYPE_MISMATCH_IN_FOR_LOOP = DiagnosticFactory2.create(ERROR, "The loop iterates over values of type {0} but the parameter is declared to be {1}");
    DiagnosticFactory1<JetElement, JetType> TYPE_MISMATCH_IN_CONDITION = DiagnosticFactory1.create(ERROR, "Condition must be of type Boolean, but was of type {0}");
    DiagnosticFactory2<JetTuplePattern, JetType, Integer> TYPE_MISMATCH_IN_TUPLE_PATTERN = DiagnosticFactory2.create(ERROR, "Type mismatch: subject is of type {0} but the pattern is of type Tuple{1}"); // TODO: message
    DiagnosticFactory2<JetTypeReference, JetType, JetType> TYPE_MISMATCH_IN_BINDING_PATTERN = DiagnosticFactory2.create(ERROR, "{0} must be a supertype of {1}. Use 'is' to match against {0}");
    DiagnosticFactory2<JetElement, JetType, JetType> INCOMPATIBLE_TYPES = DiagnosticFactory2.create(ERROR, "Incompatible types: {0} and {1}");
    DiagnosticFactory<JetWhenCondition> EXPECTED_CONDITION = DiagnosticFactory.create(ERROR, "Expected condition of Boolean type");

    DiagnosticFactory1<JetElement, JetType> CANNOT_CHECK_FOR_ERASED = DiagnosticFactory1.create(ERROR, "Cannot check for instance of erased type: {0}");
    DiagnosticFactory2<JetBinaryExpressionWithTypeRHS, JetType, JetType> UNCHECKED_CAST = DiagnosticFactory2.create(WARNING, "Unchecked cast: {0} to {1}");

    DiagnosticFactory3<JetDelegationSpecifierList, TypeParameterDescriptor, ClassDescriptor, Collection<JetType>> INCONSISTENT_TYPE_PARAMETER_VALUES =
            DiagnosticFactory3.create(ERROR, "Type parameter {0} of {1} has inconsistent values: {2}", NAME, DescriptorRenderer.TEXT, new Renderer<Collection<JetType>>() {
                @NotNull
                @Override
                public String render(@Nullable Collection<JetType> types) {
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
            });

    DiagnosticFactory3<JetBinaryExpression, JetSimpleNameExpression, JetType, JetType> EQUALITY_NOT_APPLICABLE = DiagnosticFactory3.create(ERROR, "Operator {0} cannot be applied to {1} and {2}", new Renderer<JetSimpleNameExpression>() {
        @NotNull
        @Override
        public String render(@Nullable JetSimpleNameExpression nameExpression) {
            return nameExpression.getReferencedName();
        }
    }, TO_STRING, TO_STRING);

    DiagnosticFactory2<PsiElement, CallableMemberDescriptor, DeclarationDescriptor> OVERRIDING_FINAL_MEMBER = DiagnosticFactory2.create(ERROR, "{0} in {1} is final and cannot be overridden", NAME, NAME);

    DiagnosticFactory2<JetNamedDeclaration, CallableMemberDescriptor, CallableMemberDescriptor> RETURN_TYPE_MISMATCH_ON_OVERRIDE =
            DiagnosticFactory2.create(ERROR, "Return type of {0} is not a subtype of the return type overridden member {1}", PositioningStrategies.POSITION_DECLARATION, DescriptorRenderer.TEXT, DescriptorRenderer.TEXT);

    DiagnosticFactory2<JetProperty, PropertyDescriptor, PropertyDescriptor> VAR_OVERRIDDEN_BY_VAL = DiagnosticFactory2.create(ERROR, "Var-property {0} cannot be overridden by val-property {1}", new PositioningStrategy<JetProperty>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetProperty property) {
            return markNode(property.getValOrVarNode());
        }
    }, DescriptorRenderer.TEXT, DescriptorRenderer.TEXT);

    DiagnosticFactory2<PsiElement, JetClassOrObject, CallableMemberDescriptor> ABSTRACT_MEMBER_NOT_IMPLEMENTED = DiagnosticFactory2.create(ERROR, "{0} must be declared abstract or implement abstract member {1}", RENDER_CLASS_OR_OBJECT, DescriptorRenderer.TEXT);

    DiagnosticFactory2<PsiElement, JetClassOrObject, CallableMemberDescriptor> MANY_IMPL_MEMBER_NOT_IMPLEMENTED = DiagnosticFactory2.create(ERROR, "{0} must override {1} because it inherits many implementations of it", RENDER_CLASS_OR_OBJECT, DescriptorRenderer.TEXT);

    DiagnosticFactory2<JetDeclaration, CallableMemberDescriptor, String> CONFLICTING_OVERLOADS = DiagnosticFactory2.create(ERROR, "{1} is already defined in ''{0}''", new PositioningStrategy<JetDeclaration>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetDeclaration jetDeclaration) {
            if (jetDeclaration instanceof JetNamedFunction) {
                JetNamedFunction functionElement = (JetNamedFunction) jetDeclaration;
                return markRange(new TextRange(
                        functionElement.getStartOfSignatureElement().getTextRange().getStartOffset(),
                        functionElement.getEndOfSignatureElement().getTextRange().getEndOffset()
                ));
            }
            else if (jetDeclaration instanceof JetClass) {
                // primary constructor
                JetClass klass = (JetClass) jetDeclaration;
                PsiElement nameAsDeclaration = klass.getNameIdentifier();
                if (nameAsDeclaration == null) {
                    return markRange(klass.getTextRange());
                }
                PsiElement primaryConstructorParameterList = klass.getPrimaryConstructorParameterList();
                if (primaryConstructorParameterList == null) {
                    return markRange(nameAsDeclaration.getTextRange());
                }
                return markRange(new TextRange(
                        nameAsDeclaration.getTextRange().getStartOffset(),
                        primaryConstructorParameterList.getTextRange().getEndOffset()
                ));
            }
            else {
                // safe way
                return markRange(jetDeclaration.getTextRange());
            }
        }
    }, DescriptorRenderer.TEXT, TO_STRING);


    DiagnosticFactory3<JetExpression, String, JetType, JetType> RESULT_TYPE_MISMATCH = DiagnosticFactory3.create(ERROR, "{0} must return {1} but returns {2}");
    DiagnosticFactory3<JetReferenceExpression, String, String, String> UNSAFE_INFIX_CALL = DiagnosticFactory3.create(ERROR, "Infix call corresponds to a dot-qualified call ''{0}.{1}({2})'' which is not allowed on a nullable receiver ''{0}''. Use '?.'-qualified call instead");

    AmbiguousDescriptorDiagnosticFactory OVERLOAD_RESOLUTION_AMBIGUITY = new AmbiguousDescriptorDiagnosticFactory("Overload resolution ambiguity: {0}");
    AmbiguousDescriptorDiagnosticFactory NONE_APPLICABLE = new AmbiguousDescriptorDiagnosticFactory("None of the following functions can be called with the arguments supplied: {0}");
    DiagnosticFactory1<PsiElement, ValueParameterDescriptor> NO_VALUE_FOR_PARAMETER = DiagnosticFactory1.create(ERROR, "No value passed for parameter {0}", DescriptorRenderer.TEXT);
    DiagnosticFactory1<JetReferenceExpression, JetType> MISSING_RECEIVER = DiagnosticFactory1.create(ERROR, "A receiver of type {0} is required");
    DiagnosticFactory<JetReferenceExpression> NO_RECEIVER_ADMITTED = DiagnosticFactory.create(ERROR, "No receiver can be passed to this function or property");

    DiagnosticFactory<PsiElement> CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS = DiagnosticFactory.create(ERROR, "Can not create an instance of an abstract class");
    DiagnosticFactory1<PsiElement, SolutionStatus> TYPE_INFERENCE_FAILED = DiagnosticFactory1.create(ERROR, "Type inference failed: {0}");
    DiagnosticFactory1<JetElement, Integer> WRONG_NUMBER_OF_TYPE_ARGUMENTS = DiagnosticFactory1.create(ERROR, "{0} type arguments expected", new Renderer<Integer>() {
        @NotNull
        @Override
        public String render(@Nullable Integer argument) {
            assert argument != null;
            return argument == 0 ? "No" : argument.toString();
        }
    });

    DiagnosticFactory1<JetIdeTemplateExpression, String> UNRESOLVED_IDE_TEMPLATE = DiagnosticFactory1.create(ERROR, "Unresolved IDE template: {0}");

    DiagnosticFactory<JetExpression> DANGLING_FUNCTION_LITERAL_ARGUMENT_SUSPECTED = DiagnosticFactory.create(WARNING, "This expression is treated as an argument to the function call on the previous line. " +
                                                                                                                      "Separate it with a semicolon (;) if it is not intended to be an argument.");


    // This field is needed to make the Initializer class load (interfaces cannot have static initializers)
    @SuppressWarnings("UnusedDeclaration")
    Initializer __initializer = Initializer.INSTANCE;

    class Initializer {
        static {
            for (Field field : Errors.class.getFields()) {
                if ((field.getModifiers() & Modifier.STATIC) != 0) {
                    try {
                        Object value = field.get(null);
                        if (value instanceof AbstractDiagnosticFactory) {
                            AbstractDiagnosticFactory factory = (AbstractDiagnosticFactory) value;
                            factory.setName(field.getName());
                        }
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }

        private static final Initializer INSTANCE = new Initializer();

        private Initializer() {
        }
    }
}
