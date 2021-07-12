/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve;

import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import kotlin.Pair;
import kotlin.annotations.jvm.ReadOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.cfg.LeakingThisDescriptor;
import org.jetbrains.kotlin.cfg.TailRecursionKind;
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange;
import org.jetbrains.kotlin.contracts.model.Computation;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemCompleter;
import org.jetbrains.kotlin.resolve.calls.model.PartialCallContainer;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue;
import org.jetbrains.kotlin.resolve.calls.smartcasts.ExplicitSmartCasts;
import org.jetbrains.kotlin.resolve.calls.smartcasts.ImplicitSmartCasts;
import org.jetbrains.kotlin.resolve.calls.tower.KotlinResolutionCallbacksImpl;
import org.jetbrains.kotlin.resolve.checkers.PrimitiveNumericComparisonInfo;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.resolve.scopes.receivers.Qualifier;
import org.jetbrains.kotlin.types.DeferredType;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.expressions.CaptureKind;
import org.jetbrains.kotlin.types.expressions.DoubleColonLHS;
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo;
import org.jetbrains.kotlin.types.expressions.PreliminaryDeclarationVisitor;
import org.jetbrains.kotlin.util.Box;
import org.jetbrains.kotlin.util.slicedMap.*;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.jetbrains.kotlin.util.slicedMap.RewritePolicy.DO_NOTHING;
import static org.jetbrains.kotlin.util.slicedMap.Slices.COMPILE_TIME_VALUE_REWRITE_POLICY;

public interface BindingContext {
    BindingContext EMPTY = new BindingContext() {
        @NotNull
        @Override
        public Diagnostics getDiagnostics() {
            return Diagnostics.Companion.getEMPTY();
        }

        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            return null;
        }

        @NotNull
        @Override
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            return Collections.emptyList();
        }

        @NotNull
        @TestOnly
        @Override
        public <K, V> ImmutableMap<K, V> getSliceContents(@NotNull ReadOnlySlice<K, V> slice) {
            return ImmutableMap.of();
        }

        @Nullable
        @Override
        public KotlinType getType(@NotNull KtExpression expression) {
            return null;
        }

        @Override
        public void addOwnDataTo(@NotNull BindingTrace trace, boolean commitDiagnostics) {
            // Do nothing
        }
    };

    WritableSlice<KtAnnotationEntry, AnnotationDescriptor> ANNOTATION = Slices.createSimpleSlice();

    WritableSlice<KtExpression, CompileTimeConstant<?>> COMPILE_TIME_VALUE = new BasicWritableSlice<>(COMPILE_TIME_VALUE_REWRITE_POLICY);

    WritableSlice<KtTypeReference, KotlinType> TYPE = Slices.createSimpleSlice();
    WritableSlice<KtTypeReference, KotlinType> ABBREVIATED_TYPE = Slices.createSimpleSlice();
    WritableSlice<KtExpression, KotlinTypeInfo> EXPRESSION_TYPE_INFO = new BasicWritableSlice<>(DO_NOTHING);
    WritableSlice<KtExpression, DataFlowInfo> DATA_FLOW_INFO_BEFORE = new BasicWritableSlice<>(DO_NOTHING);
    WritableSlice<KtExpression, KotlinType> EXPECTED_EXPRESSION_TYPE = new BasicWritableSlice<>(DO_NOTHING);
    WritableSlice<KtElement, Computation> EXPRESSION_EFFECTS = Slices.createSimpleSlice();
    WritableSlice<KtElement, Boolean> CONTRACT_NOT_ALLOWED = Slices.createSimpleSlice();
    WritableSlice<KtElement, Boolean> IS_CONTRACT_DECLARATION_BLOCK = Slices.createSimpleSlice();
    WritableSlice<KtFunction, KotlinType> EXPECTED_RETURN_TYPE = new BasicWritableSlice<>(DO_NOTHING);
    WritableSlice<KtExpression, DataFlowInfo> DATAFLOW_INFO_AFTER_CONDITION = Slices.createSimpleSlice();
    WritableSlice<VariableDescriptor, DataFlowValue> BOUND_INITIALIZER_VALUE = Slices.createSimpleSlice();
    WritableSlice<KtExpression, LeakingThisDescriptor> LEAKING_THIS = Slices.createSimpleSlice();
    WritableSlice<KtParameter, Boolean> UNUSED_MAIN_PARAMETER = Slices.createSimpleSlice();

    WritableSlice<VariableDescriptor, Boolean> UNUSED_DELEGATED_PROPERTY_OPERATOR_PARAMETER = Slices.createSimpleSlice();

    /**
     * A qualifier corresponds to a receiver expression (if any). For 'A.B' qualifier is recorded for 'A'.
     */
    WritableSlice<KtExpression, Qualifier> QUALIFIER = new BasicWritableSlice<>(DO_NOTHING);

    WritableSlice<KtExpression, DoubleColonLHS> DOUBLE_COLON_LHS = new BasicWritableSlice<>(DO_NOTHING);

    WritableSlice<KtSuperExpression, KotlinType> THIS_TYPE_FOR_SUPER_EXPRESSION = new BasicWritableSlice<>(DO_NOTHING);

    WritableSlice<KtReferenceExpression, DeclarationDescriptor> REFERENCE_TARGET = new BasicWritableSlice<>(DO_NOTHING);
    // if 'A' really means 'A.Companion' then this slice stores class descriptor for A, REFERENCE_TARGET stores descriptor Companion in this case
    WritableSlice<KtReferenceExpression, ClassifierDescriptorWithTypeParameters> SHORT_REFERENCE_TO_COMPANION_OBJECT =
            new BasicWritableSlice<>(DO_NOTHING);

    WritableSlice<Call, ResolvedCall<?>> RESOLVED_CALL = new BasicWritableSlice<>(DO_NOTHING);
    WritableSlice<Call, PartialCallContainer> ONLY_RESOLVED_CALL = new BasicWritableSlice<>(DO_NOTHING);
    WritableSlice<Call, BasicCallResolutionContext> PARTIAL_CALL_RESOLUTION_CONTEXT = new BasicWritableSlice<>(DO_NOTHING);
    WritableSlice<KtExpression, Call> DELEGATE_EXPRESSION_TO_PROVIDE_DELEGATE_CALL = new BasicWritableSlice<>(DO_NOTHING);
    WritableSlice<Call, TailRecursionKind> TAIL_RECURSION_CALL = Slices.createSimpleSlice();
    WritableSlice<KtElement, ConstraintSystemCompleter> CONSTRAINT_SYSTEM_COMPLETER = new BasicWritableSlice<>(DO_NOTHING);
    WritableSlice<KtElement, Call> CALL = new BasicWritableSlice<>(DO_NOTHING);

    WritableSlice<KtExpression, Collection<? extends DeclarationDescriptor>> AMBIGUOUS_REFERENCE_TARGET =
            new BasicWritableSlice<>(DO_NOTHING);

    WritableSlice<KtExpression, ResolvedCall<FunctionDescriptor>> LOOP_RANGE_ITERATOR_RESOLVED_CALL = Slices.createSimpleSlice();

    WritableSlice<KtExpression, ResolvedCall<FunctionDescriptor>> LOOP_RANGE_HAS_NEXT_RESOLVED_CALL = Slices.createSimpleSlice();
    WritableSlice<KtExpression, ResolvedCall<FunctionDescriptor>> LOOP_RANGE_NEXT_RESOLVED_CALL = Slices.createSimpleSlice();

    WritableSlice<Call, FunctionDescriptor> ENCLOSING_SUSPEND_FUNCTION_FOR_SUSPEND_FUNCTION_CALL = Slices.createSimpleSlice();

    WritableSlice<VariableAccessorDescriptor, ResolvedCall<FunctionDescriptor>> DELEGATED_PROPERTY_RESOLVED_CALL = Slices.createSimpleSlice();

    // Please consider using DELEGATED_PROPERTY_RESOLVED_CALL as it contains fully substituted types
    WritableSlice<VariableAccessorDescriptor, Call> DELEGATED_PROPERTY_CALL = Slices.createSimpleSlice();
    WritableSlice<VariableDescriptorWithAccessors, ResolvedCall<FunctionDescriptor>> PROVIDE_DELEGATE_RESOLVED_CALL = Slices.createSimpleSlice();
    WritableSlice<VariableDescriptorWithAccessors, Call> PROVIDE_DELEGATE_CALL = Slices.createSimpleSlice();

    WritableSlice<KtDestructuringDeclarationEntry, ResolvedCall<FunctionDescriptor>> COMPONENT_RESOLVED_CALL = Slices.createSimpleSlice();

    WritableSlice<KtExpression, ResolvedCall<FunctionDescriptor>> INDEXED_LVALUE_GET = Slices.createSimpleSlice();
    WritableSlice<KtExpression, ResolvedCall<FunctionDescriptor>> INDEXED_LVALUE_SET = Slices.createSimpleSlice();

    WritableSlice<KtCollectionLiteralExpression, ResolvedCall<FunctionDescriptor>> COLLECTION_LITERAL_CALL = Slices.createSimpleSlice();

    WritableSlice<KtExpression, ExplicitSmartCasts> SMARTCAST = new BasicWritableSlice<>(DO_NOTHING);
    WritableSlice<KtExpression, Boolean> SMARTCAST_NULL = Slices.createSimpleSlice();
    WritableSlice<KtExpression, ImplicitSmartCasts> IMPLICIT_RECEIVER_SMARTCAST = new BasicWritableSlice<>(DO_NOTHING);

    WritableSlice<KtWhenExpression, Boolean> EXHAUSTIVE_WHEN = Slices.createSimpleSlice();
    WritableSlice<KtWhenExpression, Boolean> IMPLICIT_EXHAUSTIVE_WHEN = Slices.createSimpleSlice();

    WritableSlice<KtElement, LexicalScope> LEXICAL_SCOPE = Slices.createSimpleSlice();

    WritableSlice<ScriptDescriptor, LexicalScope> SCRIPT_SCOPE = Slices.createSimpleSlice();

    WritableSlice<KtExpression, Boolean> VARIABLE_REASSIGNMENT = Slices.createSimpleSetSlice();
    WritableSlice<ValueParameterDescriptor, Boolean> AUTO_CREATED_IT = Slices.createSimpleSetSlice();

    WritableSlice<Pair<AnonymousFunctionDescriptor, Integer>, Boolean> SUSPEND_LAMBDA_PARAMETER_USED = Slices.createSimpleSlice();

    /**
     * Has type of current expression has been already resolved
     */
    WritableSlice<KtExpression, Boolean> PROCESSED = Slices.createSimpleSlice();
    // Please do not use this slice (USED_AS_EXPRESSION) directly,
    // use extension element.isUsedAsExpression() instead
    WritableSlice<KtElement, Boolean> USED_AS_EXPRESSION = Slices.createSimpleSetSlice();
    WritableSlice<KtElement, Boolean> USED_AS_RESULT_OF_LAMBDA = Slices.createSimpleSetSlice();

    WritableSlice<VariableDescriptor, CaptureKind> CAPTURED_IN_CLOSURE = new BasicWritableSlice<>(DO_NOTHING);
    WritableSlice<KtDeclaration, PreliminaryDeclarationVisitor> PRELIMINARY_VISITOR = new BasicWritableSlice<>(DO_NOTHING);

    WritableSlice<Box<DeferredType>, Boolean> DEFERRED_TYPE = Slices.createCollectiveSetSlice();

    WritableSlice<PropertyDescriptor, Boolean> BACKING_FIELD_REQUIRED = new SetSlice<PropertyDescriptor>(DO_NOTHING) {
        @Override
        public Boolean computeValue(
                SlicedMap map,
                PropertyDescriptor propertyDescriptor,
                Boolean backingFieldRequired,
                boolean valueNotFound
        ) {
            if (propertyDescriptor.getKind() != CallableMemberDescriptor.Kind.DECLARATION) {
                return false;
            }
            backingFieldRequired = valueNotFound ? false : backingFieldRequired;
            PsiElement declarationPsiElement = DescriptorToSourceUtils.descriptorToDeclaration(propertyDescriptor);
            if (declarationPsiElement instanceof KtParameter) {
                KtParameter jetParameter = (KtParameter) declarationPsiElement;
                return jetParameter.hasValOrVar() ||
                       backingFieldRequired; // this part is unused because we do not allow access to constructor parameters in member bodies
            }
            if (propertyDescriptor.getModality() == Modality.ABSTRACT) return false;
            if (declarationPsiElement instanceof KtProperty &&
                ((KtProperty) declarationPsiElement).hasDelegate()) return false;
            PropertyGetterDescriptor getter = propertyDescriptor.getGetter();
            PropertySetterDescriptor setter = propertyDescriptor.getSetter();

            if (getter == null) return true;
            if (propertyDescriptor.isVar() && setter == null) return true;
            if (setter != null && !DescriptorPsiUtilsKt.hasBody(setter) && setter.getModality() != Modality.ABSTRACT) return true;
            if (!DescriptorPsiUtilsKt.hasBody(getter) && getter.getModality() != Modality.ABSTRACT) return true;

            return backingFieldRequired;
        }
    };
    WritableSlice<PropertyDescriptor, Boolean> IS_UNINITIALIZED = Slices.createSimpleSetSlice();
    WritableSlice<PropertyDescriptor, Boolean> MUST_BE_LATEINIT = Slices.createSimpleSetSlice();

    WritableSlice<KtLambdaExpression, EventOccurrencesRange> LAMBDA_INVOCATIONS = Slices.createSimpleSlice();

    WritableSlice<KtLambdaExpression, Boolean> BLOCK = new SetSlice<KtLambdaExpression>(DO_NOTHING) {
        @Override
        public Boolean computeValue(SlicedMap map, KtLambdaExpression expression, Boolean isBlock, boolean valueNotFound) {
            isBlock = valueNotFound ? false : isBlock;
            return isBlock && !expression.getFunctionLiteral().hasParameterSpecification();
        }
    };

    WritableSlice<PsiElement, ClassDescriptor> CLASS = Slices.createSimpleSlice();
    WritableSlice<PsiElement, ScriptDescriptor> SCRIPT = Slices.createSimpleSlice();
    WritableSlice<KtTypeParameter, TypeParameterDescriptor> TYPE_PARAMETER = Slices.createSimpleSlice();
    /**
     * @see BindingContextUtils#recordFunctionDeclarationToDescriptor(BindingTrace, PsiElement, SimpleFunctionDescriptor)}
     */
    WritableSlice<PsiElement, SimpleFunctionDescriptor> FUNCTION = Slices.createSimpleSlice();
    WritableSlice<PsiElement, ConstructorDescriptor> CONSTRUCTOR = Slices.createSimpleSlice();
    WritableSlice<ConstructorDescriptor, ResolvedCall<ConstructorDescriptor>> CONSTRUCTOR_RESOLVED_DELEGATION_CALL =
            Slices.createSimpleSlice();
    WritableSlice<PsiElement, VariableDescriptor> VARIABLE = Slices.createSimpleSlice();
    WritableSlice<KtParameter, VariableDescriptor> VALUE_PARAMETER = Slices.createSimpleSlice();
    WritableSlice<KtPropertyAccessor, PropertyAccessorDescriptor> PROPERTY_ACCESSOR = Slices.createSimpleSlice();
    WritableSlice<PsiElement, PropertyDescriptor> PRIMARY_CONSTRUCTOR_PARAMETER = Slices.createSimpleSlice();
    WritableSlice<PsiElement, TypeAliasDescriptor> TYPE_ALIAS = Slices.createSimpleSlice();

    WritableSlice<PsiElement, Boolean> DEPRECATED_SHORT_NAME_ACCESS = Slices.createSimpleSlice();

    WritableSlice[] DECLARATIONS_TO_DESCRIPTORS = new WritableSlice[] {
            CLASS, TYPE_PARAMETER, FUNCTION, CONSTRUCTOR, VARIABLE, VALUE_PARAMETER, PROPERTY_ACCESSOR,
            PRIMARY_CONSTRUCTOR_PARAMETER, SCRIPT, TYPE_ALIAS
    };

    @SuppressWarnings("unchecked")
    ReadOnlySlice<PsiElement, DeclarationDescriptor> DECLARATION_TO_DESCRIPTOR =
            Slices.<PsiElement, DeclarationDescriptor>sliceBuilder()
                    .setFurtherLookupSlices(DECLARATIONS_TO_DESCRIPTORS)
                    .build();

    WritableSlice<DeclarationDescriptor, LinkedHashMap<ReceiverParameterDescriptor, String>> DESCRIPTOR_TO_NAMED_RECEIVERS = Slices.createSimpleSlice();
    WritableSlice<KtReferenceExpression, PsiElement> LABEL_TARGET = Slices.createSimpleSlice();
    WritableSlice<KtReferenceExpression, Collection<? extends PsiElement>> AMBIGUOUS_LABEL_TARGET = Slices.createSimpleSlice();
    WritableSlice<ValueParameterDescriptor, PropertyDescriptor> VALUE_PARAMETER_AS_PROPERTY = Slices.createSimpleSlice();

    WritableSlice<ValueParameterDescriptor, FunctionDescriptor> DATA_CLASS_COMPONENT_FUNCTION = Slices.createSimpleSlice();
    WritableSlice<ClassDescriptor, FunctionDescriptor> DATA_CLASS_COPY_FUNCTION = Slices.createSimpleSlice();

    WritableSlice<FqNameUnsafe, ClassDescriptor> FQNAME_TO_CLASS_DESCRIPTOR = new BasicWritableSlice<>(DO_NOTHING, true);
    WritableSlice<FqName, Collection<KtFile>> PACKAGE_TO_FILES = Slices.createSimpleSlice();
    WritableSlice<KtBinaryExpressionWithTypeRHS, Boolean> CAST_TYPE_USED_AS_EXPECTED_TYPE = Slices.createSimpleSlice();

    WritableSlice<KtFunction, KotlinResolutionCallbacksImpl.LambdaInfo> NEW_INFERENCE_LAMBDA_INFO = new BasicWritableSlice<>(DO_NOTHING);

    WritableSlice<KtExpression, PrimitiveNumericComparisonInfo> PRIMITIVE_NUMERIC_COMPARISON_INFO = Slices.createSimpleSlice();

    WritableSlice<KtExpression, Ref<VariableDescriptor>> NEW_INFERENCE_CATCH_EXCEPTION_PARAMETER = Slices.createSimpleSlice();
    WritableSlice<PsiElement, Boolean> NEW_INFERENCE_IS_LAMBDA_FOR_OVERLOAD_RESOLUTION_INLINE = Slices.createSimpleSlice();

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated // This field is needed only for the side effects of its initializer
            Void _static_initializer = BasicWritableSlice.initSliceDebugNames(BindingContext.class);

    @NotNull
    Diagnostics getDiagnostics();

    @Nullable
    <K, V> V get(ReadOnlySlice<K, V> slice, K key);

    // slice.isCollective() must be true
    @NotNull
    @ReadOnly
    <K, V> Collection<K> getKeys(WritableSlice<K, V> slice);

    /** This method should be used only for debug and testing */
    @TestOnly
    @NotNull
    <K, V> ImmutableMap<K, V> getSliceContents(@NotNull ReadOnlySlice<K, V> slice);

    @Nullable
    KotlinType getType(@NotNull KtExpression expression);

    void addOwnDataTo(@NotNull BindingTrace trace, boolean commitDiagnostics);
}
