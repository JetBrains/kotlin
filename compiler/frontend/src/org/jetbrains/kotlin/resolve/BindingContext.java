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

package org.jetbrains.kotlin.resolve;

import com.google.common.collect.ImmutableMap;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ReadOnly;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.cfg.LeakingThisDescriptor;
import org.jetbrains.kotlin.cfg.TailRecursionKind;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemCompleter;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue;
import org.jetbrains.kotlin.resolve.calls.smartcasts.ExplicitSmartCasts;
import org.jetbrains.kotlin.resolve.calls.smartcasts.ImplicitSmartCasts;
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

import static org.jetbrains.kotlin.util.slicedMap.RewritePolicy.DO_NOTHING;

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

    WritableSlice<KtExpression, CompileTimeConstant<?>> COMPILE_TIME_VALUE = Slices.createSimpleSlice();

    WritableSlice<KtTypeReference, KotlinType> TYPE = Slices.createSimpleSlice();
    WritableSlice<KtTypeReference, KotlinType> ABBREVIATED_TYPE = Slices.createSimpleSlice();
    WritableSlice<KtExpression, KotlinTypeInfo> EXPRESSION_TYPE_INFO = new BasicWritableSlice<KtExpression, KotlinTypeInfo>(DO_NOTHING);
    WritableSlice<KtExpression, DataFlowInfo> DATA_FLOW_INFO_BEFORE = new BasicWritableSlice<KtExpression, DataFlowInfo>(DO_NOTHING);
    WritableSlice<KtExpression, KotlinType> EXPECTED_EXPRESSION_TYPE = new BasicWritableSlice<KtExpression, KotlinType>(DO_NOTHING);
    WritableSlice<KtFunction, KotlinType> EXPECTED_RETURN_TYPE = new BasicWritableSlice<KtFunction, KotlinType>(DO_NOTHING);
    WritableSlice<KtExpression, DataFlowInfo> DATAFLOW_INFO_AFTER_CONDITION = Slices.createSimpleSlice();
    WritableSlice<VariableDescriptor, DataFlowValue> BOUND_INITIALIZER_VALUE = Slices.createSimpleSlice();
    WritableSlice<KtExpression, LeakingThisDescriptor> LEAKING_THIS = Slices.createSimpleSlice();

    /**
     * A qualifier corresponds to a receiver expression (if any). For 'A.B' qualifier is recorded for 'A'.
     */
    WritableSlice<KtExpression, Qualifier> QUALIFIER = new BasicWritableSlice<KtExpression, Qualifier>(DO_NOTHING);

    WritableSlice<KtExpression, DoubleColonLHS> DOUBLE_COLON_LHS = new BasicWritableSlice<KtExpression, DoubleColonLHS>(DO_NOTHING);

    WritableSlice<KtSuperExpression, KotlinType> THIS_TYPE_FOR_SUPER_EXPRESSION =
            new BasicWritableSlice<KtSuperExpression, KotlinType>(DO_NOTHING);

    WritableSlice<KtReferenceExpression, DeclarationDescriptor> REFERENCE_TARGET =
            new BasicWritableSlice<KtReferenceExpression, DeclarationDescriptor>(DO_NOTHING);
    // if 'A' really means 'A.Companion' then this slice stores class descriptor for A, REFERENCE_TARGET stores descriptor Companion in this case
    WritableSlice<KtReferenceExpression, ClassifierDescriptorWithTypeParameters> SHORT_REFERENCE_TO_COMPANION_OBJECT =
            new BasicWritableSlice<KtReferenceExpression, ClassifierDescriptorWithTypeParameters>(DO_NOTHING);

    WritableSlice<Call, ResolvedCall<?>> RESOLVED_CALL = new BasicWritableSlice<Call, ResolvedCall<?>>(DO_NOTHING);
    WritableSlice<Call, TailRecursionKind> TAIL_RECURSION_CALL = Slices.createSimpleSlice();
    WritableSlice<KtElement, ConstraintSystemCompleter> CONSTRAINT_SYSTEM_COMPLETER = new BasicWritableSlice<KtElement, ConstraintSystemCompleter>(DO_NOTHING);
    WritableSlice<KtElement, Call> CALL = new BasicWritableSlice<KtElement, Call>(DO_NOTHING);

    WritableSlice<KtExpression, Collection<? extends DeclarationDescriptor>> AMBIGUOUS_REFERENCE_TARGET =
            new BasicWritableSlice<KtExpression, Collection<? extends DeclarationDescriptor>>(DO_NOTHING);

    WritableSlice<KtExpression, ResolvedCall<FunctionDescriptor>> LOOP_RANGE_ITERATOR_RESOLVED_CALL = Slices.createSimpleSlice();

    WritableSlice<KtExpression, ResolvedCall<FunctionDescriptor>> LOOP_RANGE_HAS_NEXT_RESOLVED_CALL = Slices.createSimpleSlice();
    WritableSlice<KtExpression, ResolvedCall<FunctionDescriptor>> LOOP_RANGE_NEXT_RESOLVED_CALL = Slices.createSimpleSlice();
    WritableSlice<KtExpression, ResolvedCall<FunctionDescriptor>> RETURN_HANDLE_RESULT_RESOLVED_CALL = Slices.createSimpleSlice();

    WritableSlice<Call, FunctionDescriptor> ENCLOSING_SUSPEND_FUNCTION_FOR_SUSPEND_FUNCTION_CALL = Slices.createSimpleSlice();
    WritableSlice<FunctionDescriptor, Boolean> CONTAINS_NON_TAIL_SUSPEND_CALLS = Slices.createSimpleSetSlice();
    WritableSlice<KtExpression, Boolean> IS_TAIL_EXPRESSION_IN_SUSPEND_FUNCTION = Slices.createSimpleSetSlice();

    WritableSlice<VariableAccessorDescriptor, ResolvedCall<FunctionDescriptor>> DELEGATED_PROPERTY_RESOLVED_CALL = Slices.createSimpleSlice();
    WritableSlice<VariableAccessorDescriptor, Call> DELEGATED_PROPERTY_CALL = Slices.createSimpleSlice();
    WritableSlice<VariableDescriptorWithAccessors, ResolvedCall<FunctionDescriptor>> PROVIDE_DELEGATE_RESOLVED_CALL = Slices.createSimpleSlice();
    WritableSlice<VariableDescriptorWithAccessors, Call> PROVIDE_DELEGATE_CALL = Slices.createSimpleSlice();

    WritableSlice<KtDestructuringDeclarationEntry, ResolvedCall<FunctionDescriptor>> COMPONENT_RESOLVED_CALL = Slices.createSimpleSlice();

    WritableSlice<KtExpression, ResolvedCall<FunctionDescriptor>> INDEXED_LVALUE_GET = Slices.createSimpleSlice();
    WritableSlice<KtExpression, ResolvedCall<FunctionDescriptor>> INDEXED_LVALUE_SET = Slices.createSimpleSlice();

    WritableSlice<KtExpression, ExplicitSmartCasts> SMARTCAST = new BasicWritableSlice<KtExpression, ExplicitSmartCasts>(DO_NOTHING);
    WritableSlice<KtExpression, Boolean> SMARTCAST_NULL = Slices.createSimpleSlice();
    WritableSlice<KtExpression, ImplicitSmartCasts> IMPLICIT_RECEIVER_SMARTCAST = new BasicWritableSlice<KtExpression, ImplicitSmartCasts>(DO_NOTHING);

    WritableSlice<KtWhenExpression, Boolean> EXHAUSTIVE_WHEN = Slices.createSimpleSlice();
    WritableSlice<KtWhenExpression, Boolean> IMPLICIT_EXHAUSTIVE_WHEN = Slices.createSimpleSlice();

    WritableSlice<KtElement, LexicalScope> LEXICAL_SCOPE = Slices.createSimpleSlice();

    WritableSlice<ScriptDescriptor, LexicalScope> SCRIPT_SCOPE = Slices.createSimpleSlice();

    WritableSlice<KtExpression, Boolean> VARIABLE_REASSIGNMENT = Slices.createSimpleSetSlice();
    WritableSlice<ValueParameterDescriptor, Boolean> AUTO_CREATED_IT = Slices.createSimpleSetSlice();

    /**
     * Has type of current expression has been already resolved
     */
    WritableSlice<KtExpression, Boolean> PROCESSED = Slices.createSimpleSlice();
    WritableSlice<KtElement, Boolean> USED_AS_EXPRESSION = Slices.createSimpleSetSlice();
    WritableSlice<KtElement, Boolean> USED_AS_RESULT_OF_LAMBDA = Slices.createSimpleSetSlice();
    WritableSlice<KtElement, Boolean> UNREACHABLE_CODE = Slices.createSimpleSetSlice();

    WritableSlice<VariableDescriptor, CaptureKind> CAPTURED_IN_CLOSURE = new BasicWritableSlice<VariableDescriptor, CaptureKind>(DO_NOTHING);
    WritableSlice<KtDeclaration, PreliminaryDeclarationVisitor> PRELIMINARY_VISITOR = new BasicWritableSlice<KtDeclaration, PreliminaryDeclarationVisitor>(DO_NOTHING);

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

    WritableSlice[] DECLARATIONS_TO_DESCRIPTORS = new WritableSlice[] {
            CLASS, TYPE_PARAMETER, FUNCTION, CONSTRUCTOR, VARIABLE, VALUE_PARAMETER, PROPERTY_ACCESSOR,
            PRIMARY_CONSTRUCTOR_PARAMETER, SCRIPT, TYPE_ALIAS
    };

    @SuppressWarnings("unchecked")
    ReadOnlySlice<PsiElement, DeclarationDescriptor> DECLARATION_TO_DESCRIPTOR =
            Slices.<PsiElement, DeclarationDescriptor>sliceBuilder()
                    .setFurtherLookupSlices(DECLARATIONS_TO_DESCRIPTORS)
                    .build();

    WritableSlice<KtReferenceExpression, PsiElement> LABEL_TARGET = Slices.createSimpleSlice();
    WritableSlice<KtReferenceExpression, Collection<? extends PsiElement>> AMBIGUOUS_LABEL_TARGET = Slices.createSimpleSlice();
    WritableSlice<ValueParameterDescriptor, PropertyDescriptor> VALUE_PARAMETER_AS_PROPERTY = Slices.createSimpleSlice();

    WritableSlice<ValueParameterDescriptor, FunctionDescriptor> DATA_CLASS_COMPONENT_FUNCTION = Slices.createSimpleSlice();
    WritableSlice<ClassDescriptor, FunctionDescriptor> DATA_CLASS_COPY_FUNCTION = Slices.createSimpleSlice();

    WritableSlice<FqNameUnsafe, ClassDescriptor> FQNAME_TO_CLASS_DESCRIPTOR =
            new BasicWritableSlice<FqNameUnsafe, ClassDescriptor>(DO_NOTHING, true);
    WritableSlice<KtFile, PackageFragmentDescriptor> FILE_TO_PACKAGE_FRAGMENT = Slices.createSimpleSlice();
    WritableSlice<FqName, Collection<KtFile>> PACKAGE_TO_FILES = Slices.createSimpleSlice();

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
