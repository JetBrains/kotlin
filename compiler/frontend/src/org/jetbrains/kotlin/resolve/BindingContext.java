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

package org.jetbrains.kotlin.resolve;

import com.google.common.collect.ImmutableMap;
import com.intellij.psi.PsiElement;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ReadOnly;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.cfg.TailRecursionKind;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemCompleter;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.resolve.scopes.receivers.Qualifier;
import org.jetbrains.kotlin.types.Approximation;
import org.jetbrains.kotlin.types.DeferredType;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.expressions.CaptureKind;
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
            return Diagnostics.EMPTY;
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
    };

    WritableSlice<AnnotationDescriptor, JetAnnotationEntry> ANNOTATION_DESCRIPTOR_TO_PSI_ELEMENT = Slices.createSimpleSlice();
    WritableSlice<JetAnnotationEntry, AnnotationDescriptor> ANNOTATION =
            Slices.<JetAnnotationEntry, AnnotationDescriptor>sliceBuilder().setOpposite(ANNOTATION_DESCRIPTOR_TO_PSI_ELEMENT).build();

    WritableSlice<JetExpression, CompileTimeConstant<?>> COMPILE_TIME_VALUE = Slices.createSimpleSlice();

    WritableSlice<JetTypeReference, JetType> TYPE = Slices.createSimpleSlice();
    WritableSlice<JetExpression, JetType> EXPRESSION_TYPE = new BasicWritableSlice<JetExpression, JetType>(DO_NOTHING);
    WritableSlice<JetExpression, JetType> EXPECTED_EXPRESSION_TYPE = new BasicWritableSlice<JetExpression, JetType>(DO_NOTHING);
    WritableSlice<JetFunction, JetType> EXPECTED_RETURN_TYPE = new BasicWritableSlice<JetFunction, JetType>(DO_NOTHING);
    WritableSlice<JetExpression, DataFlowInfo> EXPRESSION_DATA_FLOW_INFO = new BasicWritableSlice<JetExpression, DataFlowInfo>(DO_NOTHING);
    WritableSlice<JetExpression, Approximation.Info> EXPRESSION_RESULT_APPROXIMATION = new BasicWritableSlice<JetExpression, Approximation.Info>(DO_NOTHING);
    WritableSlice<JetExpression, DataFlowInfo> DATAFLOW_INFO_AFTER_CONDITION = Slices.createSimpleSlice();

    /**
     * A qualifier corresponds to a receiver expression (if any). For 'A.B' qualifier is recorded for 'A'.
     */
    WritableSlice<JetExpression, Qualifier> QUALIFIER = new BasicWritableSlice<JetExpression, Qualifier>(DO_NOTHING);

    WritableSlice<JetReferenceExpression, DeclarationDescriptor> REFERENCE_TARGET =
            new BasicWritableSlice<JetReferenceExpression, DeclarationDescriptor>(DO_NOTHING);
    // if 'A' really means 'A.Default' then this slice stores class descriptor for A, REFERENCE_TARGET stores descriptor Default in this case
    WritableSlice<JetReferenceExpression, ClassDescriptor> SHORT_REFERENCE_TO_DEFAULT_OBJECT =
            new BasicWritableSlice<JetReferenceExpression, ClassDescriptor>(DO_NOTHING);

    @KotlinSignature("val RESOLVED_CALL: WritableSlice<Call, ResolvedCall<out CallableDescriptor>>")
    WritableSlice<Call, ResolvedCall<?>> RESOLVED_CALL = new BasicWritableSlice<Call, ResolvedCall<?>>(DO_NOTHING);
    WritableSlice<ResolvedCall<?>, TailRecursionKind> TAIL_RECURSION_CALL = Slices.createSimpleSlice();
    WritableSlice<JetElement, ConstraintSystemCompleter> CONSTRAINT_SYSTEM_COMPLETER = new BasicWritableSlice<JetElement, ConstraintSystemCompleter>(DO_NOTHING);
    WritableSlice<JetElement, Call> CALL = new BasicWritableSlice<JetElement, Call>(DO_NOTHING);

    @KotlinSignature("val AMBIGUOUS_REFERENCE_TARGET: WritableSlice<JetExpression, Collection<DeclarationDescriptor>>")
    WritableSlice<JetExpression, Collection<? extends DeclarationDescriptor>> AMBIGUOUS_REFERENCE_TARGET =
            new BasicWritableSlice<JetExpression, Collection<? extends DeclarationDescriptor>>(DO_NOTHING);

    WritableSlice<JetExpression, DelegatingBindingTrace> TRACE_DELTAS_CACHE = Slices.createSimpleSlice();

    WritableSlice<JetExpression, ResolvedCall<FunctionDescriptor>> LOOP_RANGE_ITERATOR_RESOLVED_CALL = Slices.createSimpleSlice();

    WritableSlice<JetExpression, ResolvedCall<FunctionDescriptor>> LOOP_RANGE_HAS_NEXT_RESOLVED_CALL = Slices.createSimpleSlice();
    WritableSlice<JetExpression, ResolvedCall<FunctionDescriptor>> LOOP_RANGE_NEXT_RESOLVED_CALL = Slices.createSimpleSlice();

    WritableSlice<PropertyAccessorDescriptor, ResolvedCall<FunctionDescriptor>> DELEGATED_PROPERTY_RESOLVED_CALL = Slices.createSimpleSlice();
    WritableSlice<PropertyAccessorDescriptor, Call> DELEGATED_PROPERTY_CALL = Slices.createSimpleSlice();

    WritableSlice<PropertyDescriptor, ResolvedCall<FunctionDescriptor>> DELEGATED_PROPERTY_PD_RESOLVED_CALL = Slices.createSimpleSlice();

    WritableSlice<JetMultiDeclarationEntry, ResolvedCall<FunctionDescriptor>> COMPONENT_RESOLVED_CALL = Slices.createSimpleSlice();

    WritableSlice<JetExpression, ResolvedCall<FunctionDescriptor>> INDEXED_LVALUE_GET = Slices.createSimpleSlice();
    WritableSlice<JetExpression, ResolvedCall<FunctionDescriptor>> INDEXED_LVALUE_SET = Slices.createSimpleSlice();

    WritableSlice<JetExpression, JetType> SMARTCAST = Slices.createSimpleSlice();

    WritableSlice<JetWhenExpression, Boolean> EXHAUSTIVE_WHEN = Slices.createSimpleSlice();

    /**
     * A scope where type of expression has been resolved
     */
    WritableSlice<JetTypeReference, JetScope> TYPE_RESOLUTION_SCOPE = Slices.createSimpleSlice();
    WritableSlice<JetExpression, JetScope> RESOLUTION_SCOPE = Slices.createSimpleSlice();

    WritableSlice<ScriptDescriptor, JetScope> SCRIPT_SCOPE = Slices.createSimpleSlice();

    WritableSlice<JetExpression, Boolean> VARIABLE_REASSIGNMENT = Slices.createSimpleSetSlice();
    WritableSlice<ValueParameterDescriptor, Boolean> AUTO_CREATED_IT = Slices.createSimpleSetSlice();

    /**
     * Has type of current expression has been already resolved
     */
    WritableSlice<JetExpression, Boolean> PROCESSED = Slices.createSimpleSetSlice();
    WritableSlice<JetElement, Boolean> USED_AS_EXPRESSION = Slices.createRemovableSetSlice();
    WritableSlice<JetElement, Boolean> UNREACHABLE_CODE = Slices.createRemovableSetSlice();

    WritableSlice<VariableDescriptor, CaptureKind> CAPTURED_IN_CLOSURE = new BasicWritableSlice<VariableDescriptor, CaptureKind>(DO_NOTHING);

    WritableSlice<CallableMemberDescriptor, Boolean> NEED_SYNTHETIC_ACCESSOR = new BasicWritableSlice<CallableMemberDescriptor, Boolean>(DO_NOTHING);

    WritableSlice<Box<DeferredType>, Boolean> DEFERRED_TYPE = Slices.createCollectiveSetSlice();

    WritableSlice<PropertyDescriptor, Boolean> BACKING_FIELD_REQUIRED = new Slices.SetSlice<PropertyDescriptor>(DO_NOTHING) {
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
            if (declarationPsiElement instanceof JetParameter) {
                JetParameter jetParameter = (JetParameter) declarationPsiElement;
                return jetParameter.hasValOrVarNode() ||
                       backingFieldRequired; // this part is unused because we do not allow access to constructor parameters in member bodies
            }
            if (propertyDescriptor.getModality() == Modality.ABSTRACT) return false;
            PropertyGetterDescriptor getter = propertyDescriptor.getGetter();
            PropertySetterDescriptor setter = propertyDescriptor.getSetter();

            if (getter == null) return true;
            if (propertyDescriptor.isVar() && setter == null) return true;
            if (setter != null && !setter.hasBody() && setter.getModality() != Modality.ABSTRACT) return true;
            if (!getter.hasBody() && getter.getModality() != Modality.ABSTRACT) return true;

            return backingFieldRequired;
        }
    };
    WritableSlice<PropertyDescriptor, Boolean> IS_INITIALIZED = Slices.createSimpleSetSlice();

    WritableSlice<JetFunctionLiteralExpression, Boolean> BLOCK = new Slices.SetSlice<JetFunctionLiteralExpression>(DO_NOTHING) {
        @Override
        public Boolean computeValue(SlicedMap map, JetFunctionLiteralExpression expression, Boolean isBlock, boolean valueNotFound) {
            isBlock = valueNotFound ? false : isBlock;
            assert isBlock != null;
            return isBlock && !expression.getFunctionLiteral().hasParameterSpecification();
        }
    };

    WritableSlice<PsiElement, ClassDescriptor> CLASS = Slices.<PsiElement, ClassDescriptor>sliceBuilder().build();
    WritableSlice<PsiElement, ScriptDescriptor> SCRIPT = Slices.<PsiElement, ScriptDescriptor>sliceBuilder().build();
    WritableSlice<JetTypeParameter, TypeParameterDescriptor> TYPE_PARAMETER =
            Slices.<JetTypeParameter, TypeParameterDescriptor>sliceBuilder().build();
    /**
     * @see BindingContextUtils#recordFunctionDeclarationToDescriptor(BindingTrace, PsiElement, SimpleFunctionDescriptor)}
     */
    WritableSlice<PsiElement, SimpleFunctionDescriptor> FUNCTION = Slices.<PsiElement, SimpleFunctionDescriptor>sliceBuilder().build();
    WritableSlice<PsiElement, ConstructorDescriptor> CONSTRUCTOR = Slices.<PsiElement, ConstructorDescriptor>sliceBuilder().build();
    WritableSlice<PsiElement, VariableDescriptor> VARIABLE = Slices.<PsiElement, VariableDescriptor>sliceBuilder().build();
    WritableSlice<JetParameter, VariableDescriptor> VALUE_PARAMETER = Slices.<JetParameter, VariableDescriptor>sliceBuilder().build();
    WritableSlice<JetPropertyAccessor, PropertyAccessorDescriptor> PROPERTY_ACCESSOR =
            Slices.<JetPropertyAccessor, PropertyAccessorDescriptor>sliceBuilder().build();
    WritableSlice<PsiElement, PropertyDescriptor> PRIMARY_CONSTRUCTOR_PARAMETER =
            Slices.<PsiElement, PropertyDescriptor>sliceBuilder().build();

    WritableSlice[] DECLARATIONS_TO_DESCRIPTORS = new WritableSlice[] {
            CLASS, TYPE_PARAMETER, FUNCTION, CONSTRUCTOR, VARIABLE, VALUE_PARAMETER, PROPERTY_ACCESSOR,
            PRIMARY_CONSTRUCTOR_PARAMETER
    };

    ReadOnlySlice<PsiElement, DeclarationDescriptor> DECLARATION_TO_DESCRIPTOR = Slices.<PsiElement, DeclarationDescriptor>sliceBuilder()
            .setFurtherLookupSlices(DECLARATIONS_TO_DESCRIPTORS).build();

    WritableSlice<JetReferenceExpression, PsiElement> LABEL_TARGET = Slices.<JetReferenceExpression, PsiElement>sliceBuilder().build();
    @KotlinSignature("val AMBIGUOUS_LABEL_TARGET: WritableSlice<JetReferenceExpression, Collection<PsiElement>>")
    WritableSlice<JetReferenceExpression, Collection<? extends PsiElement>> AMBIGUOUS_LABEL_TARGET =
            Slices.<JetReferenceExpression, Collection<? extends PsiElement>>sliceBuilder().build();
    WritableSlice<ValueParameterDescriptor, PropertyDescriptor> VALUE_PARAMETER_AS_PROPERTY =
            Slices.<ValueParameterDescriptor, PropertyDescriptor>sliceBuilder().build();

    WritableSlice<ValueParameterDescriptor, FunctionDescriptor> DATA_CLASS_COMPONENT_FUNCTION =
            Slices.<ValueParameterDescriptor, FunctionDescriptor>sliceBuilder().build();
    WritableSlice<ClassDescriptor, FunctionDescriptor> DATA_CLASS_COPY_FUNCTION =
            Slices.<ClassDescriptor, FunctionDescriptor>sliceBuilder().build();

    WritableSlice<FqNameUnsafe, ClassDescriptor> FQNAME_TO_CLASS_DESCRIPTOR = new BasicWritableSlice<FqNameUnsafe, ClassDescriptor>(DO_NOTHING, true);
    WritableSlice<JetFile, PackageFragmentDescriptor> FILE_TO_PACKAGE_FRAGMENT = Slices.createSimpleSlice();
    WritableSlice<FqName, Collection<JetFile>> PACKAGE_TO_FILES = Slices.createSimpleSlice();

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
}
