package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExtensionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.jet.lang.types.DescriptorSubstitutor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.lang.types.Variance;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor.NO_RECEIVER;

/**
 * @author abreslav
 */
public class PropertyDescriptor extends VariableDescriptorImpl implements CallableMemberDescriptor {

    private final Modality modality;
    private final Visibility visibility;
    private final boolean isVar;
    private final Set<PropertyDescriptor> overriddenProperties = Sets.newLinkedHashSet();
    private final PropertyDescriptor original;

    private ReceiverDescriptor expectedThisObject;
    private ReceiverDescriptor receiver;
    private List<TypeParameterDescriptor> typeParemeters;
    private PropertyGetterDescriptor getter;
    private PropertySetterDescriptor setter;

    private PropertyDescriptor(
            @Nullable PropertyDescriptor original,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull Modality modality,
            @NotNull Visibility visibility,
            boolean isVar,
            @NotNull String name) {
        super(containingDeclaration, annotations, name);
        this.isVar = isVar;
        this.modality = modality;
        this.visibility = visibility;
        this.original = original == null ? this : original.getOriginal();
    }

    public PropertyDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull Modality modality,
            @NotNull Visibility visibility,
            boolean isVar,
            @NotNull String name) {
        this(null, containingDeclaration, annotations, modality, visibility, isVar, name);
    }

    public PropertyDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull Modality modality,
            @NotNull Visibility visibility,
            boolean isVar,
            @Nullable JetType receiverType,
            @NotNull ReceiverDescriptor expectedThisObject,
            @NotNull String name,
            @Nullable JetType inType,
            @NotNull JetType outType
        ) {
        this(containingDeclaration, annotations, modality, visibility, isVar, name);
        setType(inType, outType, Collections.<TypeParameterDescriptor>emptyList(), expectedThisObject, receiverType);
    }

    private PropertyDescriptor(
            @NotNull PropertyDescriptor original,
            @Nullable JetType receiverType,
            @NotNull ReceiverDescriptor expectedThisObject,
            @Nullable JetType inType,
            @NotNull JetType outType
        ) {
        this(
                original,
                original.getContainingDeclaration(),
                original.getAnnotations(), // TODO : substitute?
                original.getModality(),
                original.getVisibility(),
                original.isVar,
                original.getName()
        );
        setType(inType, outType, Collections.<TypeParameterDescriptor>emptyList(), expectedThisObject, receiverType);
    }

    public void setType(@Nullable JetType inType, @NotNull JetType outType, @NotNull List<TypeParameterDescriptor> typeParameters, @NotNull ReceiverDescriptor expectedThisObject, @Nullable JetType receiverType) {
        ReceiverDescriptor receiver = receiverType == null
                ? NO_RECEIVER
                : new ExtensionReceiver(this, receiverType);
        setType(inType, outType, typeParameters, expectedThisObject, receiver);
    }

    public void setType(@Nullable JetType inType, @NotNull JetType outType, @NotNull List<TypeParameterDescriptor> typeParameters, @NotNull ReceiverDescriptor expectedThisObject, @NotNull ReceiverDescriptor receiver) {
        assert !isVar || inType != null;
        setInType(inType);
        setOutType(outType);

        this.typeParemeters = typeParameters;

        this.receiver = receiver;
        this.expectedThisObject = expectedThisObject;
    }

    public void initialize(@Nullable PropertyGetterDescriptor getter, @Nullable PropertySetterDescriptor setter) {
        this.getter = getter;
        this.setter = setter;
    }

    @NotNull
    @Override
    public List<TypeParameterDescriptor> getTypeParameters() {
        return typeParemeters;
    }

    @NotNull
    public ReceiverDescriptor getReceiverParameter() {
        return receiver;
    }

    @NotNull
    @Override
    public ReceiverDescriptor getExpectedThisObject() {
        return expectedThisObject;
    }

    @Override
    public JetType getReturnType() {
        return getOutType();
    }

    @Override
    public JetType getInType() {
        return super.getInType();
    }

    @Override
    @NotNull
    public JetType getOutType() {
        return super.getOutType();
    }

    public boolean isVar() {
        return isVar;
    }

    @NotNull
    @Override
    public Modality getModality() {
        return modality;
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return visibility;
    }

    @Nullable
    public PropertyGetterDescriptor getGetter() {
        return getter;
    }

    @Nullable
    public PropertySetterDescriptor getSetter() {
        return setter;
    }

    @Override
    public PropertyDescriptor substitute(TypeSubstitutor originalSubstitutor) {
        if (originalSubstitutor.isEmpty()) {
            return this;
        }
        PropertyDescriptor substitutedDescriptor = new PropertyDescriptor(this, getContainingDeclaration(), getAnnotations(), getModality(), getVisibility(), isVar(), getName());

        List<TypeParameterDescriptor> substitutedTypeParameters = Lists.newArrayList();
        TypeSubstitutor substitutor = DescriptorSubstitutor.substituteTypeParameters(getTypeParameters(), originalSubstitutor, substitutedDescriptor, substitutedTypeParameters);

        JetType originalInType = getInType();
        JetType inType = originalInType == null ? null : substitutor.substitute(originalInType, Variance.IN_VARIANCE);
        JetType originalOutType = getOutType();
        JetType outType = substitutor.substitute(originalOutType, Variance.OUT_VARIANCE);
        if (inType == null && outType == null) {
            return null; // TODO : tell the user that the property was projected out
        }

        ReceiverDescriptor substitutedExpectedThisObject;
        if (expectedThisObject.exists()) {
            JetType substitutedExpectedThisObjectType = substitutor.substitute(getExpectedThisObject().getType(), Variance.INVARIANT);
            substitutedExpectedThisObject = new TransientReceiver(substitutedExpectedThisObjectType);
        }
        else {
            substitutedExpectedThisObject = NO_RECEIVER;
        }

        JetType substitutedReceiverType;
        if (receiver.exists()) {
            substitutedReceiverType = substitutor.substitute(receiver.getType(), Variance.IN_VARIANCE);
            if (substitutedReceiverType == null) return null;
        }
        else {
            substitutedReceiverType = null;
        }

        substitutedDescriptor.setType(inType, outType, substitutedTypeParameters, substitutedExpectedThisObject, substitutedReceiverType);

        return substitutedDescriptor;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitPropertyDescriptor(this, data);
    }

    @NotNull
    @Override
    public PropertyDescriptor getOriginal() {
        return original;
    }

    public void addOverriddenDescriptor(PropertyDescriptor overridden) {
        overriddenProperties.add(overridden);
    }

    @NotNull
    @Override
    public Set<? extends PropertyDescriptor> getOverriddenDescriptors() {
        return overriddenProperties;
    }

    @NotNull
    @Override
    public PropertyDescriptor copy(DeclarationDescriptor newOwner, boolean makeNonAbstract) {
        PropertyDescriptor propertyDescriptor = new PropertyDescriptor(
                newOwner,
                Lists.newArrayList(getAnnotations()),
                DescriptorUtils.convertModality(modality, makeNonAbstract), visibility, isVar,
                getName());

        propertyDescriptor.setType(getInType(), getOutType(), DescriptorUtils.copyTypeParameters(propertyDescriptor, getTypeParameters()), expectedThisObject, receiver.exists() ? receiver.getType() : null);

        PropertyGetterDescriptor newGetter = getter == null ? null : new PropertyGetterDescriptor(
                propertyDescriptor, Lists.newArrayList(getter.getAnnotations()),
                DescriptorUtils.convertModality(getter.getModality(), makeNonAbstract), getter.getVisibility(),
                getter.hasBody(), getter.isDefault());
        if (newGetter != null) {
            newGetter.initialize(getter.getReturnType());
        }
        PropertySetterDescriptor newSetter = setter == null ? null : new PropertySetterDescriptor(
                        DescriptorUtils.convertModality(setter.getModality(), makeNonAbstract), setter.getVisibility(),
                        propertyDescriptor,
                        Lists.newArrayList(setter.getAnnotations()),
                        setter.hasBody(), setter.isDefault());
        propertyDescriptor.initialize(newGetter, newSetter);
        return propertyDescriptor;
    }
}
