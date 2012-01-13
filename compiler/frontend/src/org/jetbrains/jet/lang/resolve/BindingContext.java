package org.jetbrains.jet.lang.resolve;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.DeferredType;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.util.Box;
import org.jetbrains.jet.util.slicedmap.*;

import java.util.Collection;

import static org.jetbrains.jet.util.slicedmap.RewritePolicy.DO_NOTHING;

/**
 * @author abreslav
 */
public interface BindingContext {
    WritableSlice<JetAnnotationEntry, AnnotationDescriptor> ANNOTATION = Slices.createSimpleSlice();

    WritableSlice<JetExpression, CompileTimeConstant<?>> COMPILE_TIME_VALUE = Slices.createSimpleSlice();
    WritableSlice<JetTypeReference, JetType> TYPE = Slices.createSimpleSlice();
    WritableSlice<JetExpression, JetType> EXPRESSION_TYPE = new BasicWritableSlice<JetExpression, JetType>(DO_NOTHING);

    WritableSlice<JetReferenceExpression, DeclarationDescriptor> REFERENCE_TARGET = new BasicWritableSlice<JetReferenceExpression, DeclarationDescriptor>(DO_NOTHING);
    WritableSlice<JetElement, ResolvedCall<? extends CallableDescriptor>> RESOLVED_CALL = new BasicWritableSlice<JetElement, ResolvedCall<? extends CallableDescriptor>>(DO_NOTHING);

    WritableSlice<JetReferenceExpression, Collection<? extends DeclarationDescriptor>> AMBIGUOUS_REFERENCE_TARGET = new BasicWritableSlice<JetReferenceExpression, Collection<? extends DeclarationDescriptor>>(DO_NOTHING);

    WritableSlice<JetExpression, FunctionDescriptor> LOOP_RANGE_ITERATOR = Slices.createSimpleSlice();
    WritableSlice<JetExpression, CallableDescriptor> LOOP_RANGE_HAS_NEXT = Slices.createSimpleSlice();
    WritableSlice<JetExpression, FunctionDescriptor> LOOP_RANGE_NEXT = Slices.createSimpleSlice();

    WritableSlice<JetExpression, ResolvedCall<FunctionDescriptor>> INDEXED_LVALUE_GET = Slices.createSimpleSlice();
    WritableSlice<JetExpression, ResolvedCall<FunctionDescriptor>> INDEXED_LVALUE_SET = Slices.createSimpleSlice();

    WritableSlice<JetExpression, JetType> AUTOCAST = Slices.createSimpleSlice();

    /** A scope where type of expression has been resolved */
    WritableSlice<JetExpression, JetScope> RESOLUTION_SCOPE = Slices.createSimpleSlice();

    WritableSlice<JetExpression, Boolean> VARIABLE_REASSIGNMENT = Slices.createSimpleSetSlice();
    WritableSlice<ValueParameterDescriptor, Boolean> AUTO_CREATED_IT = Slices.createSimpleSetSlice();
    WritableSlice<JetExpression, DeclarationDescriptor> VARIABLE_ASSIGNMENT = Slices.createSimpleSlice();

    /** Has type of current expression has been already resolved */
    WritableSlice<JetExpression, Boolean> PROCESSED = Slices.createSimpleSetSlice();
    WritableSlice<JetElement, Boolean> STATEMENT = Slices.createRemovableSetSlice();
    WritableSlice<CallableMemberDescriptor, Boolean> DELEGATED = Slices.createRemovableSetSlice();

    WritableSlice<VariableDescriptor, Boolean> MUST_BE_WRAPPED_IN_A_REF = Slices.createSimpleSetSlice();
    
//    enum DeferredTypeKey {DEFERRED_TYPE_KEY}
//    WritableSlice<DeferredTypeKey, Collection<DeferredType>> DEFERRED_TYPES = Slices.createSimpleSlice();

    WritableSlice<Box<DeferredType>, Boolean> DEFERRED_TYPE = Slices.createCollectiveSetSlice();

    WritableSlice<PropertyDescriptor, Boolean> BACKING_FIELD_REQUIRED = new Slices.SetSlice<PropertyDescriptor>(DO_NOTHING) {
        @Override
        public Boolean computeValue(SlicedMap map, PropertyDescriptor propertyDescriptor, Boolean backingFieldRequired, boolean valueNotFound) {
            backingFieldRequired = valueNotFound ? false : backingFieldRequired;
            assert backingFieldRequired != null;
            PsiElement declarationPsiElement = map.get(DESCRIPTOR_TO_DECLARATION, propertyDescriptor);
            if (declarationPsiElement instanceof JetParameter) {
                JetParameter jetParameter = (JetParameter) declarationPsiElement;
                return jetParameter.getValOrVarNode() != null ||
                       backingFieldRequired; // this part is unused because we do not allow access to constructor parameters in member bodies
            }
            if (propertyDescriptor.getModality() == Modality.ABSTRACT) return false;
            PropertyGetterDescriptor getter = propertyDescriptor.getGetter();
            PropertySetterDescriptor setter = propertyDescriptor.getSetter();
            if (getter == null) {
                return true;
            }
            else if (propertyDescriptor.isVar() && setter == null) {
                return true;
            }
            else if (setter != null && !setter.hasBody() && setter.getModality() != Modality.ABSTRACT) {
                return true;
            }
            else if (!getter.hasBody() && getter.getModality() != Modality.ABSTRACT) {
                return true;
            }
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

    Slices.KeyNormalizer<DeclarationDescriptor> DECLARATION_DESCRIPTOR_NORMALIZER = new Slices.KeyNormalizer<DeclarationDescriptor>() {
        @Override
        public DeclarationDescriptor normalize(DeclarationDescriptor declarationDescriptor) {
            if (declarationDescriptor instanceof VariableAsFunctionDescriptor) {
                VariableAsFunctionDescriptor descriptor = (VariableAsFunctionDescriptor) declarationDescriptor;
                return descriptor.getVariableDescriptor().getOriginal();
            }
            return declarationDescriptor.getOriginal();
        }
    };
    ReadOnlySlice<DeclarationDescriptor, PsiElement> DESCRIPTOR_TO_DECLARATION = Slices.<DeclarationDescriptor, PsiElement>sliceBuilder().setKeyNormalizer(DECLARATION_DESCRIPTOR_NORMALIZER).build();

    WritableSlice<PsiElement, NamespaceDescriptor> NAMESPACE = Slices.<PsiElement, NamespaceDescriptor>sliceBuilder().setOpposite((WritableSlice) DESCRIPTOR_TO_DECLARATION).build();
    WritableSlice<PsiElement, ClassDescriptor> CLASS = Slices.<PsiElement, ClassDescriptor>sliceBuilder().setOpposite((WritableSlice) DESCRIPTOR_TO_DECLARATION).build();
    WritableSlice<JetTypeParameter, TypeParameterDescriptor> TYPE_PARAMETER = Slices.<JetTypeParameter, TypeParameterDescriptor>sliceBuilder().setOpposite((WritableSlice) DESCRIPTOR_TO_DECLARATION).build();
    WritableSlice<PsiElement, NamedFunctionDescriptor> FUNCTION = Slices.<PsiElement, NamedFunctionDescriptor>sliceBuilder().setOpposite((WritableSlice) DESCRIPTOR_TO_DECLARATION).build();
    WritableSlice<PsiElement, ConstructorDescriptor> CONSTRUCTOR = Slices.<PsiElement, ConstructorDescriptor>sliceBuilder().setOpposite((WritableSlice) DESCRIPTOR_TO_DECLARATION).build();
    WritableSlice<PsiElement, VariableDescriptor> VARIABLE = Slices.<PsiElement, VariableDescriptor>sliceBuilder().setOpposite((WritableSlice) DESCRIPTOR_TO_DECLARATION).build();
    WritableSlice<JetParameter, VariableDescriptor> VALUE_PARAMETER = Slices.<JetParameter, VariableDescriptor>sliceBuilder().setOpposite((WritableSlice) DESCRIPTOR_TO_DECLARATION).build();
    WritableSlice<JetPropertyAccessor, PropertyAccessorDescriptor> PROPERTY_ACCESSOR = Slices.<JetPropertyAccessor, PropertyAccessorDescriptor>sliceBuilder().setOpposite((WritableSlice) DESCRIPTOR_TO_DECLARATION).build();

    // normalize value to getOriginal(value)
    WritableSlice<PsiElement, PropertyDescriptor> PRIMARY_CONSTRUCTOR_PARAMETER = Slices.<PsiElement, PropertyDescriptor>sliceBuilder().setOpposite((WritableSlice) DESCRIPTOR_TO_DECLARATION).build();
    WritableSlice<JetObjectDeclarationName, PropertyDescriptor> OBJECT_DECLARATION = Slices.<JetObjectDeclarationName, PropertyDescriptor>sliceBuilder().setOpposite((WritableSlice) DESCRIPTOR_TO_DECLARATION).build();

    WritableSlice[] DECLARATIONS_TO_DESCRIPTORS = new WritableSlice[] {
        NAMESPACE, CLASS, TYPE_PARAMETER, FUNCTION, CONSTRUCTOR, VARIABLE, VALUE_PARAMETER, PROPERTY_ACCESSOR, PRIMARY_CONSTRUCTOR_PARAMETER, OBJECT_DECLARATION
    };

    ReadOnlySlice<PsiElement, DeclarationDescriptor> DECLARATION_TO_DESCRIPTOR = Slices.<PsiElement, DeclarationDescriptor>sliceBuilder()
            .setFurtherLookupSlices(DECLARATIONS_TO_DESCRIPTORS).build();

    WritableSlice<JetReferenceExpression, PsiElement> LABEL_TARGET = Slices.<JetReferenceExpression, PsiElement>sliceBuilder().build();
    WritableSlice<JetParameter, PropertyDescriptor> VALUE_PARAMETER_AS_PROPERTY = Slices.<JetParameter, PropertyDescriptor>sliceBuilder().build();

    WritableSlice<String, ClassDescriptor> FQNAME_TO_CLASS_DESCRIPTOR = new BasicWritableSlice<String, ClassDescriptor>(DO_NOTHING, true);
    WritableSlice<String, NamespaceDescriptor> FQNAME_TO_NAMESPACE_DESCRIPTOR = new BasicWritableSlice<String, NamespaceDescriptor>(DO_NOTHING);

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated // This field is needed only for the side effects of its initializer
    Void _static_initializer = BasicWritableSlice.initSliceDebugNames(BindingContext.class);
    
    Collection<Diagnostic> getDiagnostics();

    @Nullable
    <K, V> V get(ReadOnlySlice<K, V> slice, K key);

    // slice.isCollective() must be true
    @NotNull
    <K, V> Collection<K> getKeys(WritableSlice<K, V> slice);
}
