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
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.inference.SolutionStatus;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetTokens;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Severity.ERROR;
import static org.jetbrains.jet.lang.diagnostics.Severity.WARNING;

/**
 * For error messages, see DefaultErrorMessages and IdeErrorMessages.
 *
 * @author abreslav
 */
public interface Errors {

    DiagnosticFactory1<JetFile, Throwable> EXCEPTION_WHILE_ANALYZING = DiagnosticFactory1.create(ERROR);

    UnresolvedReferenceDiagnosticFactory UNRESOLVED_REFERENCE = UnresolvedReferenceDiagnosticFactory.create();

    //Elements with "INVISIBLE_REFERENCE" error are marked as unresolved, unlike elements with "INVISIBLE_MEMBER" error
    DiagnosticFactory2<JetSimpleNameExpression, DeclarationDescriptor, DeclarationDescriptor> INVISIBLE_REFERENCE =
            DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<PsiElement, DeclarationDescriptor, DeclarationDescriptor> INVISIBLE_MEMBER = DiagnosticFactory2.create(ERROR);

    RedeclarationDiagnosticFactory REDECLARATION = new RedeclarationDiagnosticFactory(ERROR);
    RedeclarationDiagnosticFactory NAME_SHADOWING = new RedeclarationDiagnosticFactory(WARNING);

    DiagnosticFactory2<PsiElement, JetType, JetType> TYPE_MISMATCH = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<PsiElement, Collection<JetKeywordToken>> INCOMPATIBLE_MODIFIERS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, JetKeywordToken> ILLEGAL_MODIFIER = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory2<PsiElement, JetKeywordToken, JetKeywordToken> REDUNDANT_MODIFIER = DiagnosticFactory2.create(Severity.WARNING);
    SimpleDiagnosticFactory<JetModifierListOwner> ABSTRACT_MODIFIER_IN_TRAIT = SimpleDiagnosticFactory
            .create(WARNING, PositioningStrategies.POSITION_ABSTRACT_MODIFIER);
    SimpleDiagnosticFactory<JetModifierListOwner> OPEN_MODIFIER_IN_TRAIT = SimpleDiagnosticFactory
            .create(WARNING, PositioningStrategies.positionModifier(JetTokens.OPEN_KEYWORD));
    SimpleDiagnosticFactory<PsiElement>
            REDUNDANT_MODIFIER_IN_GETTER = SimpleDiagnosticFactory.create(WARNING);
    SimpleDiagnosticFactory<PsiElement> TRAIT_CAN_NOT_BE_FINAL = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetExpression> TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetReturnExpression> RETURN_NOT_ALLOWED = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetTypeProjection> PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE =
            SimpleDiagnosticFactory.create(ERROR, PositioningStrategies.PROJECTION_MODIFIER);
    SimpleDiagnosticFactory<JetSimpleNameExpression>LABEL_NAME_CLASH = SimpleDiagnosticFactory.create(WARNING);
    SimpleDiagnosticFactory<JetSimpleNameExpression> EXPRESSION_EXPECTED_NAMESPACE_FOUND = SimpleDiagnosticFactory.create(ERROR);

    DiagnosticFactory1<JetSimpleNameExpression, DeclarationDescriptor> CANNOT_IMPORT_FROM_ELEMENT = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetSimpleNameExpression, DeclarationDescriptor> CANNOT_BE_IMPORTED = DiagnosticFactory1.create(ERROR);
    SimpleDiagnosticFactory<JetExpression>USELESS_HIDDEN_IMPORT = SimpleDiagnosticFactory.create(WARNING);
    SimpleDiagnosticFactory<JetExpression> USELESS_SIMPLE_IMPORT = SimpleDiagnosticFactory.create(WARNING);

    SimpleDiagnosticFactory<JetParameter> CANNOT_INFER_PARAMETER_TYPE = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<JetElement> NO_BACKING_FIELD_ABSTRACT_PROPERTY = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetElement> NO_BACKING_FIELD_CUSTOM_ACCESSORS = SimpleDiagnosticFactory.create(ERROR
    );
    SimpleDiagnosticFactory<JetElement> INACCESSIBLE_BACKING_FIELD = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetElement> NOT_PROPERTY_BACKING_FIELD = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<PsiElement> MIXING_NAMED_AND_POSITIONED_ARGUMENTS = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetReferenceExpression> ARGUMENT_PASSED_TWICE = SimpleDiagnosticFactory.create(ERROR);
    UnresolvedReferenceDiagnosticFactory NAMED_PARAMETER_NOT_FOUND = UnresolvedReferenceDiagnosticFactory.create();
    SimpleDiagnosticFactory<JetExpression> VARARG_OUTSIDE_PARENTHESES = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<LeafPsiElement> NON_VARARG_SPREAD = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<JetExpression> MANY_FUNCTION_LITERAL_ARGUMENTS = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<PsiElement> PROPERTY_WITH_NO_TYPE_NO_INITIALIZER = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<JetModifierListOwner> ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS = SimpleDiagnosticFactory
            .create(ERROR, PositioningStrategies.POSITION_ABSTRACT_MODIFIER);
    SimpleDiagnosticFactory<JetProperty> ABSTRACT_PROPERTY_NOT_IN_CLASS = SimpleDiagnosticFactory.create(ERROR,
                                                                                                         PositioningStrategies.POSITION_ABSTRACT_MODIFIER);
    SimpleDiagnosticFactory<JetExpression> ABSTRACT_PROPERTY_WITH_INITIALIZER = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetPropertyAccessor> ABSTRACT_PROPERTY_WITH_GETTER = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetPropertyAccessor>ABSTRACT_PROPERTY_WITH_SETTER = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<PsiElement> PACKAGE_MEMBER_CANNOT_BE_PROTECTED = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<PsiElement> GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetProperty> BACKING_FIELD_IN_TRAIT = SimpleDiagnosticFactory.create(ERROR,
                                                                                                 PositioningStrategies.POSITION_NAME_IDENTIFIER);
    SimpleDiagnosticFactory<JetProperty> MUST_BE_INITIALIZED = SimpleDiagnosticFactory.create(ERROR,
                                                                                              PositioningStrategies.POSITION_NAME_IDENTIFIER);
    SimpleDiagnosticFactory<JetProperty> MUST_BE_INITIALIZED_OR_BE_ABSTRACT = SimpleDiagnosticFactory.create(ERROR,
                                                                                                             PositioningStrategies.POSITION_NAME_IDENTIFIER);
    SimpleDiagnosticFactory<JetExpression>PROPERTY_INITIALIZER_IN_TRAIT = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetExpression> PROPERTY_INITIALIZER_NO_BACKING_FIELD = SimpleDiagnosticFactory.create(ERROR);
    DiagnosticFactory2<JetModifierListOwner, String, ClassDescriptor> ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS =
            DiagnosticFactory2.create(ERROR, PositioningStrategies.POSITION_ABSTRACT_MODIFIER);
    DiagnosticFactory2<JetFunction, String, ClassDescriptor> ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS =
            DiagnosticFactory2.create(ERROR,PositioningStrategies.POSITION_ABSTRACT_MODIFIER);
    DiagnosticFactory1<JetFunction, SimpleFunctionDescriptor> ABSTRACT_FUNCTION_WITH_BODY =
            DiagnosticFactory1.create(ERROR, PositioningStrategies.POSITION_ABSTRACT_MODIFIER);
    DiagnosticFactory1<JetFunction, SimpleFunctionDescriptor> NON_ABSTRACT_FUNCTION_WITH_NO_BODY =
            DiagnosticFactory1.create(ERROR, PositioningStrategies.POSITION_NAME_IDENTIFIER);
    DiagnosticFactory1<JetModifierListOwner, SimpleFunctionDescriptor> NON_MEMBER_ABSTRACT_FUNCTION =
            DiagnosticFactory1.create(ERROR, PositioningStrategies.POSITION_ABSTRACT_MODIFIER);

    DiagnosticFactory1<JetFunction, SimpleFunctionDescriptor> NON_MEMBER_FUNCTION_NO_BODY =
            DiagnosticFactory1.create(ERROR, PositioningStrategies.POSITION_NAME_IDENTIFIER);
    SimpleDiagnosticFactory<JetNamedDeclaration> NON_FINAL_MEMBER_IN_FINAL_CLASS =
            SimpleDiagnosticFactory.create(ERROR, PositioningStrategies.positionModifier(JetTokens.OPEN_KEYWORD));

    SimpleDiagnosticFactory<JetNamedDeclaration> PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE =
            SimpleDiagnosticFactory.create(ERROR, PositioningStrategies.POSITION_NAME_IDENTIFIER);

    SimpleDiagnosticFactory<JetTypeProjection> PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT =
            SimpleDiagnosticFactory.create(ERROR, PositioningStrategies.PROJECTION_MODIFIER);
    SimpleDiagnosticFactory<JetDelegatorToSuperClass> SUPERTYPE_NOT_INITIALIZED = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetDelegatorToSuperClass> SUPERTYPE_NOT_INITIALIZED_DEFAULT = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<PsiElement> SECONDARY_CONSTRUCTOR_BUT_NO_PRIMARY = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<PsiElement> SECONDARY_CONSTRUCTOR_NO_INITIALIZER_LIST = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetDelegatorByExpressionSpecifier> BY_IN_SECONDARY_CONSTRUCTOR = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetDelegatorToSuperClass> INITIALIZER_WITH_NO_ARGUMENTS = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetDelegationSpecifier> MANY_CALLS_TO_THIS = SimpleDiagnosticFactory.create(ERROR);
    DiagnosticFactory1<JetModifierListOwner, CallableMemberDescriptor> NOTHING_TO_OVERRIDE =
            DiagnosticFactory1.create(ERROR, PositioningStrategies.positionModifier(JetTokens.OVERRIDE_KEYWORD));
    DiagnosticFactory3<PsiNameIdentifierOwner, CallableMemberDescriptor, CallableMemberDescriptor, DeclarationDescriptor>
            VIRTUAL_MEMBER_HIDDEN = DiagnosticFactory3.create(ERROR, PositioningStrategies.POSITION_NAME_IDENTIFIER);

    DiagnosticFactory1<JetClass, ClassDescriptor> ENUM_ENTRY_SHOULD_BE_INITIALIZED =
            DiagnosticFactory1.create(ERROR, PositioningStrategies.POSITION_NAME_IDENTIFIER);
    DiagnosticFactory1<JetTypeReference, ClassDescriptor> ENUM_ENTRY_ILLEGAL_TYPE = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<JetSimpleNameExpression, VariableDescriptor> UNINITIALIZED_VARIABLE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetSimpleNameExpression, ValueParameterDescriptor> UNINITIALIZED_PARAMETER = DiagnosticFactory1.create(ERROR);
    UnusedElementDiagnosticFactory<JetProperty, VariableDescriptor> UNUSED_VARIABLE =
            UnusedElementDiagnosticFactory.create(WARNING, PositioningStrategies.POSITION_NAME_IDENTIFIER);
    UnusedElementDiagnosticFactory<JetParameter, VariableDescriptor> UNUSED_PARAMETER =
            UnusedElementDiagnosticFactory.create(WARNING, PositioningStrategies.POSITION_NAME_IDENTIFIER);
    UnusedElementDiagnosticFactory<JetNamedDeclaration, DeclarationDescriptor> ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE =
            UnusedElementDiagnosticFactory.create(WARNING, PositioningStrategies.POSITION_NAME_IDENTIFIER);
    DiagnosticFactory1<JetExpression, DeclarationDescriptor> VARIABLE_WITH_REDUNDANT_INITIALIZER = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory2<JetElement, JetElement, DeclarationDescriptor> UNUSED_VALUE = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory1<JetElement, JetElement> UNUSED_CHANGED_VALUE = DiagnosticFactory1.create(WARNING);
    SimpleDiagnosticFactory<JetElement> UNUSED_EXPRESSION = SimpleDiagnosticFactory.create(WARNING);
    SimpleDiagnosticFactory<JetFunctionLiteralExpression> UNUSED_FUNCTION_LITERAL = SimpleDiagnosticFactory.create(WARNING);

    DiagnosticFactory1<JetExpression, DeclarationDescriptor> VAL_REASSIGNMENT = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetExpression, DeclarationDescriptor> INITIALIZATION_BEFORE_DECLARATION = DiagnosticFactory1.create(ERROR);
    SimpleDiagnosticFactory<JetExpression> VARIABLE_EXPECTED = SimpleDiagnosticFactory.create(ERROR);

    DiagnosticFactory1<JetExpression, DeclarationDescriptor> INITIALIZATION_USING_BACKING_FIELD_CUSTOM_SETTER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetExpression, DeclarationDescriptor> INITIALIZATION_USING_BACKING_FIELD_OPEN_SETTER = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<JetSimpleNameExpression, DeclarationDescriptor> FUNCTION_PARAMETERS_OF_INLINE_FUNCTION = DiagnosticFactory1.create(ERROR);

    SimpleDiagnosticFactory<JetElement> UNREACHABLE_CODE = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<JetClassObject> MANY_CLASS_OBJECTS = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetClassObject> CLASS_OBJECT_NOT_ALLOWED = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetDelegatorByExpressionSpecifier> DELEGATION_IN_TRAIT = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetTypeReference> DELEGATION_NOT_TO_TRAIT = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<PsiElement> NO_CONSTRUCTOR = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetExpression> NOT_A_CLASS = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetEscapeStringTemplateEntry> ILLEGAL_ESCAPE_SEQUENCE = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<JetTypeReference> LOCAL_EXTENSION_PROPERTY = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetPropertyAccessor> LOCAL_VARIABLE_WITH_GETTER = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetPropertyAccessor> LOCAL_VARIABLE_WITH_SETTER = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetPropertyAccessor> VAL_WITH_SETTER = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<JetArrayAccessExpression> NO_GET_METHOD =
            SimpleDiagnosticFactory.create(ERROR, PositioningStrategies.POSITION_ARRAY_ACCESS);
    SimpleDiagnosticFactory<JetArrayAccessExpression> NO_SET_METHOD =
            SimpleDiagnosticFactory.create(ERROR, PositioningStrategies.POSITION_ARRAY_ACCESS);

    SimpleDiagnosticFactory<JetSimpleNameExpression> INC_DEC_SHOULD_NOT_RETURN_UNIT = SimpleDiagnosticFactory.create(ERROR);
    DiagnosticFactory2<JetSimpleNameExpression, DeclarationDescriptor, JetSimpleNameExpression> ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT =
            DiagnosticFactory2.create(ERROR);
    AmbiguousDescriptorDiagnosticFactory ASSIGN_OPERATOR_AMBIGUITY = AmbiguousDescriptorDiagnosticFactory.create();

    SimpleDiagnosticFactory<JetSimpleNameExpression> EQUALS_MISSING = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetBinaryExpression> ASSIGNMENT_IN_EXPRESSION_CONTEXT = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetRootNamespaceExpression> NAMESPACE_IS_NOT_AN_EXPRESSION = SimpleDiagnosticFactory.create(ERROR);
    DiagnosticFactory1<JetSuperExpression, String> SUPER_IS_NOT_AN_EXPRESSION = DiagnosticFactory1.create(ERROR);
    SimpleDiagnosticFactory<JetDeclaration> DECLARATION_IN_ILLEGAL_CONTEXT = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetExpression> SETTER_PARAMETER_WITH_DEFAULT_VALUE = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetThisExpression> NO_THIS = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetSuperExpression> SUPER_NOT_AVAILABLE = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetSuperExpression> AMBIGUOUS_SUPER = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetExpression> ABSTRACT_SUPER_CALL = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetTypeReference> NOT_A_SUPERTYPE = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<PsiElement> TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER = SimpleDiagnosticFactory.create(WARNING);
    SimpleDiagnosticFactory<JetSimpleNameExpression> USELESS_CAST_STATIC_ASSERT_IS_FINE = SimpleDiagnosticFactory.create(WARNING);
    SimpleDiagnosticFactory<JetSimpleNameExpression> USELESS_CAST = SimpleDiagnosticFactory.create(WARNING);
    SimpleDiagnosticFactory<JetSimpleNameExpression> CAST_NEVER_SUCCEEDS = SimpleDiagnosticFactory.create(WARNING);
    DiagnosticFactory2<JetTypeReference, JetType, JetType> WRONG_SETTER_PARAMETER_TYPE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<JetTypeReference, JetType, JetType> WRONG_GETTER_RETURN_TYPE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<JetSimpleNameExpression, ClassifierDescriptor> NO_CLASS_OBJECT = DiagnosticFactory1.create(ERROR);
    SimpleDiagnosticFactory<PsiElement> NO_GENERICS_IN_SUPERTYPE_SPECIFIER = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<JetExpression> HAS_NEXT_PROPERTY_AND_FUNCTION_AMBIGUITY = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetExpression> HAS_NEXT_MISSING = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetExpression> HAS_NEXT_FUNCTION_AMBIGUITY = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetExpression> HAS_NEXT_MUST_BE_READABLE = SimpleDiagnosticFactory.create(ERROR);
    DiagnosticFactory1<JetExpression, JetType> HAS_NEXT_PROPERTY_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetExpression, JetType> HAS_NEXT_FUNCTION_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    SimpleDiagnosticFactory<JetExpression> NEXT_AMBIGUITY = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetExpression> NEXT_MISSING = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetExpression> ITERATOR_MISSING = SimpleDiagnosticFactory.create(ERROR);
    AmbiguousDescriptorDiagnosticFactory ITERATOR_AMBIGUITY = AmbiguousDescriptorDiagnosticFactory.create();

    DiagnosticFactory1<JetSimpleNameExpression, JetType> COMPARE_TO_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetExpression, JetType> CALLEE_NOT_A_FUNCTION = DiagnosticFactory1.create(ERROR);

    SimpleDiagnosticFactory<JetReturnExpression> RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetDeclarationWithBody> NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY =
            SimpleDiagnosticFactory.create(ERROR,
                                           new PositioningStrategy<JetDeclarationWithBody>() {
                                               @NotNull
                                               @Override
                                               public List<TextRange> mark(@NotNull JetDeclarationWithBody element) {
                                                   JetExpression bodyExpression = element.getBodyExpression();
                                                   if (!(bodyExpression instanceof JetBlockExpression)) {
                                                       return Collections.emptyList();
                                                   }
                                                   JetBlockExpression blockExpression = (JetBlockExpression)bodyExpression;
                                                   TextRange lastBracketRange = blockExpression.getLastBracketRange();
                                                   if (lastBracketRange == null) {
                                                       return Collections.emptyList();
                                                   }
                                                   return markRange(lastBracketRange);
                                               }
                                           });
    DiagnosticFactory1<JetExpression, JetType> RETURN_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetExpression, JetType> EXPECTED_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetBinaryExpression, JetType> ASSIGNMENT_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetExpression, JetType> IMPLICIT_CAST_TO_UNIT_OR_ANY = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory1<JetExpression, JetExpression> EXPRESSION_EXPECTED = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory2<JetTypeReference, JetType, JetType> UPPER_BOUND_VIOLATED = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<JetTypeReference, JetType> FINAL_CLASS_OBJECT_UPPER_BOUND = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetTypeReference, JetType> FINAL_UPPER_BOUND = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory1<JetExpression, JetType> USELESS_ELVIS = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory1<PsiElement, TypeParameterDescriptor> CONFLICTING_UPPER_BOUNDS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, TypeParameterDescriptor> CONFLICTING_CLASS_OBJECT_UPPER_BOUNDS = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<PsiElement, CallableDescriptor> TOO_MANY_ARGUMENTS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, String> ERROR_COMPILE_TIME_VALUE = DiagnosticFactory1.create(ERROR);

    SimpleDiagnosticFactory<JetWhenEntry> ELSE_MISPLACED_IN_WHEN = SimpleDiagnosticFactory.create(ERROR, new PositioningStrategy<JetWhenEntry>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetWhenEntry entry) {
            PsiElement elseKeywordElement = entry.getElseKeywordElement();
            assert elseKeywordElement != null;
            return markElement(elseKeywordElement);
        }
    });

    SimpleDiagnosticFactory<JetWhenExpression>
            NO_ELSE_IN_WHEN = new SimpleDiagnosticFactory<JetWhenExpression>(ERROR, new PositioningStrategy<JetWhenExpression>() {
        @NotNull
        @Override
        public List<TextRange> mark(@NotNull JetWhenExpression element) {
            if (hasSyntaxError(element)) return Collections.emptyList();
            return markElement(element.getWhenKeywordElement());
        }
    });
    SimpleDiagnosticFactory<JetWhenConditionInRange> TYPE_MISMATCH_IN_RANGE =
            new SimpleDiagnosticFactory<JetWhenConditionInRange>(ERROR,
                                                                 new PositioningStrategy<JetWhenConditionInRange>() {
                                                                     @NotNull
                                                                     @Override
                                                                     public List<TextRange> mark(@NotNull JetWhenConditionInRange condition) {
                                                                         return markElement(condition.getOperationReference());
                                                                     }
                                                                 });
    SimpleDiagnosticFactory<PsiElement> CYCLIC_INHERITANCE_HIERARCHY = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<JetTypeReference> MANY_CLASSES_IN_SUPERTYPE_LIST = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetTypeReference> SUPERTYPE_NOT_A_CLASS_OR_TRAIT = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<PsiElement> SUPERTYPE_INITIALIZED_IN_TRAIT = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<PsiElement> CONSTRUCTOR_IN_TRAIT = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetSecondaryConstructor> SECONDARY_CONSTRUCTORS_ARE_NOT_SUPPORTED = SimpleDiagnosticFactory.create(WARNING);
    SimpleDiagnosticFactory<JetTypeReference> SUPERTYPE_APPEARS_TWICE = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetTypeReference> FINAL_SUPERTYPE = SimpleDiagnosticFactory.create(ERROR);

    DiagnosticFactory1<JetExpression, String> ILLEGAL_SELECTOR = DiagnosticFactory1.create(ERROR);

    SimpleDiagnosticFactory<JetParameter> VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetLabelQualifiedExpression> BREAK_OR_CONTINUE_OUTSIDE_A_LOOP = SimpleDiagnosticFactory.create(ERROR);
    DiagnosticFactory1<JetLabelQualifiedExpression, String> NOT_A_LOOP_LABEL = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetReturnExpression, String> NOT_A_RETURN_LABEL = DiagnosticFactory1.create(ERROR);

    SimpleDiagnosticFactory<JetClassInitializer> ANONYMOUS_INITIALIZER_WITHOUT_CONSTRUCTOR = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetNullableType> NULLABLE_SUPERTYPE = SimpleDiagnosticFactory.create(ERROR,
                                                                                                 new PositioningStrategy<JetNullableType>() {
                                                                                                     @NotNull
                                                                                                     @Override
                                                                                                     public List<TextRange> mark(@NotNull JetNullableType element) {
                                                                                                         return markNode(
                                                                                                                 element.getQuestionMarkNode());
                                                                                                     }
                                                                                                 });
    DiagnosticFactory1<PsiElement, JetType> UNSAFE_CALL = DiagnosticFactory1.create(ERROR);
    SimpleDiagnosticFactory<JetSimpleNameExpression> AMBIGUOUS_LABEL = SimpleDiagnosticFactory.create(ERROR);
    DiagnosticFactory1<PsiElement, String> UNSUPPORTED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, JetType> UNNECESSARY_SAFE_CALL = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory1<PsiElement, JetType> UNNECESSARY_NOT_NULL_ASSERTION = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory2<JetSimpleNameExpression, JetTypeConstraint, JetTypeParameterListOwner> NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER =
            DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<JetExpression, JetType, String> AUTOCAST_IMPOSSIBLE = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory2<JetTypeReference, JetType, JetType> TYPE_MISMATCH_IN_FOR_LOOP = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<JetElement, JetType> TYPE_MISMATCH_IN_CONDITION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<JetTuplePattern, JetType, Integer> TYPE_MISMATCH_IN_TUPLE_PATTERN = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<JetTypeReference, JetType, JetType> TYPE_MISMATCH_IN_BINDING_PATTERN = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<JetElement, JetType, JetType> INCOMPATIBLE_TYPES = DiagnosticFactory2.create(ERROR);
    SimpleDiagnosticFactory<JetWhenCondition> EXPECTED_CONDITION = SimpleDiagnosticFactory.create(ERROR);

    DiagnosticFactory1<JetElement, JetType> CANNOT_CHECK_FOR_ERASED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<JetBinaryExpressionWithTypeRHS, JetType, JetType> UNCHECKED_CAST = DiagnosticFactory2.create(WARNING);

    DiagnosticFactory3<JetDelegationSpecifierList, TypeParameterDescriptor, ClassDescriptor, Collection<JetType>>
            INCONSISTENT_TYPE_PARAMETER_VALUES = DiagnosticFactory3.create(ERROR);

    DiagnosticFactory3<JetBinaryExpression, JetSimpleNameExpression, JetType, JetType> EQUALITY_NOT_APPLICABLE =
            DiagnosticFactory3.create(ERROR);

    DiagnosticFactory2<PsiElement, CallableMemberDescriptor, DeclarationDescriptor> OVERRIDING_FINAL_MEMBER =
            DiagnosticFactory2.create(ERROR);
    DiagnosticFactory3<JetModifierListOwner, Visibility, CallableMemberDescriptor, DeclarationDescriptor> CANNOT_WEAKEN_ACCESS_PRIVILEGE =
            DiagnosticFactory3.create(ERROR, PositioningStrategies.POSITION_VISIBILITY_MODIFIER);
    DiagnosticFactory3<JetModifierListOwner, Visibility, CallableMemberDescriptor, DeclarationDescriptor> CANNOT_CHANGE_ACCESS_PRIVILEGE =
            DiagnosticFactory3.create(ERROR, PositioningStrategies.POSITION_VISIBILITY_MODIFIER);

    DiagnosticFactory2<JetNamedDeclaration, CallableMemberDescriptor, CallableMemberDescriptor> RETURN_TYPE_MISMATCH_ON_OVERRIDE =
            DiagnosticFactory2.create(ERROR, PositioningStrategies.POSITION_DECLARATION);

    DiagnosticFactory2<JetProperty, PropertyDescriptor, PropertyDescriptor> VAR_OVERRIDDEN_BY_VAL =
            DiagnosticFactory2.create(ERROR, new PositioningStrategy<JetProperty>() {
                @NotNull
                @Override
                public List<TextRange> mark(@NotNull JetProperty property) {
                    return markNode(property.getValOrVarNode());
                }
            });

    DiagnosticFactory2<PsiElement, JetClassOrObject, CallableMemberDescriptor> ABSTRACT_MEMBER_NOT_IMPLEMENTED =
            DiagnosticFactory2.create(ERROR);

    DiagnosticFactory2<PsiElement, JetClassOrObject, CallableMemberDescriptor> MANY_IMPL_MEMBER_NOT_IMPLEMENTED =
            DiagnosticFactory2.create(ERROR);

    DiagnosticFactory2<JetDeclaration, CallableMemberDescriptor, String> CONFLICTING_OVERLOADS =
            DiagnosticFactory2.create(ERROR, new PositioningStrategy<JetDeclaration>() {
                @NotNull
                @Override
                public List<TextRange> mark(@NotNull JetDeclaration jetDeclaration) {
                    if (jetDeclaration instanceof JetNamedFunction) {
                        JetNamedFunction functionElement = (JetNamedFunction)jetDeclaration;
                        return markRange(new TextRange(
                                functionElement.getStartOfSignatureElement().getTextRange().getStartOffset(),
                                functionElement.getEndOfSignatureElement().getTextRange().getEndOffset()
                        ));
                    }
                    else if (jetDeclaration instanceof JetClass) {
                        // primary constructor
                        JetClass klass = (JetClass)jetDeclaration;
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
            });


    DiagnosticFactory3<JetExpression, String, JetType, JetType> RESULT_TYPE_MISMATCH = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory3<JetReferenceExpression, String, String, String> UNSAFE_INFIX_CALL = DiagnosticFactory3.create(ERROR);

    AmbiguousDescriptorDiagnosticFactory OVERLOAD_RESOLUTION_AMBIGUITY = new AmbiguousDescriptorDiagnosticFactory();
    AmbiguousDescriptorDiagnosticFactory NONE_APPLICABLE = new AmbiguousDescriptorDiagnosticFactory();
    DiagnosticFactory1<PsiElement, ValueParameterDescriptor> NO_VALUE_FOR_PARAMETER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetReferenceExpression, JetType> MISSING_RECEIVER = DiagnosticFactory1.create(ERROR);
    SimpleDiagnosticFactory<JetReferenceExpression> NO_RECEIVER_ADMITTED = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<PsiElement> CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS = SimpleDiagnosticFactory.create(ERROR);
    DiagnosticFactory1<PsiElement, SolutionStatus> TYPE_INFERENCE_FAILED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetElement, Integer> WRONG_NUMBER_OF_TYPE_ARGUMENTS = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<JetIdeTemplateExpression, String> UNRESOLVED_IDE_TEMPLATE = DiagnosticFactory1.create(ERROR);

    SimpleDiagnosticFactory<JetExpression> DANGLING_FUNCTION_LITERAL_ARGUMENT_SUSPECTED = SimpleDiagnosticFactory.create(WARNING);

    DiagnosticFactory1<JetAnnotationEntry, String> NOT_AN_ANNOTATION_CLASS = DiagnosticFactory1.create(ERROR);


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
                            AbstractDiagnosticFactory factory = (AbstractDiagnosticFactory)value;
                            factory.setName(field.getName());
                        }
                    }
                    catch (IllegalAccessException e) {
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
