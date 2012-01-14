package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.resolve.AbstractScopeAdapter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.TraceBasedRedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.*;

/**
 * @author abreslav
 */
public class MutableClassDescriptor extends MutableClassDescriptorLite {
    private final Set<CallableMemberDescriptor> callableMembers = Sets.newHashSet();
    private final Set<PropertyDescriptor> properties = Sets.newHashSet();
    private final Set<NamedFunctionDescriptor> functions = Sets.newHashSet();

    private final WritableScope scopeForMemberResolution;
    // This scope contains type parameters but does not contain inner classes
    private final WritableScope scopeForSupertypeResolution;
    private final WritableScope scopeForInitializers; //contains members + primary constructor value parameters + map for backing fields

    public MutableClassDescriptor(@NotNull BindingTrace trace, @NotNull DeclarationDescriptor containingDeclaration, @NotNull JetScope outerScope, ClassKind kind) {
        super(containingDeclaration, kind, new TraceBasedRedeclarationHandler(trace));

        if (containingDeclaration instanceof ClassDescriptor
                || containingDeclaration instanceof NamespaceLike
                || containingDeclaration instanceof ModuleDescriptor
                || containingDeclaration instanceof FunctionDescriptor)
        {
        } else {
            throw new IllegalStateException();
        }

        TraceBasedRedeclarationHandler redeclarationHandler = new TraceBasedRedeclarationHandler(trace);
        this.scopeForSupertypeResolution = new WritableScopeImpl(outerScope, this, redeclarationHandler).setDebugName("SupertypeResolution").changeLockLevel(WritableScope.LockLevel.BOTH);
        this.scopeForMemberResolution = new WritableScopeImpl(scopeForSupertypeResolution, this, redeclarationHandler).setDebugName("MemberResolution").changeLockLevel(WritableScope.LockLevel.BOTH);
        this.scopeForInitializers = new WritableScopeImpl(scopeForMemberResolution, this, redeclarationHandler).setDebugName("Initializers").changeLockLevel(WritableScope.LockLevel.BOTH);
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    public ClassObjectStatus setClassObjectDescriptor(@NotNull final MutableClassDescriptor classObjectDescriptor) {
        ClassObjectStatus r = super.setClassObjectDescriptor(classObjectDescriptor);
        if (r != ClassObjectStatus.OK) {
            return r;
        }

        // Members of the class object are accessible from the class
        // The scope must be lazy, because classObjectDescriptor may not by fully built yet
        scopeForMemberResolution.importScope(new AbstractScopeAdapter() {
            @NotNull
            @Override
            protected JetScope getWorkerScope() {
                return classObjectDescriptor.getDefaultType().getMemberScope();
            }

            @NotNull
            @Override
            public ReceiverDescriptor getImplicitReceiver() {
                return classObjectDescriptor.getImplicitReceiver();
            }
        }
        );

        return ClassObjectStatus.OK;
    }

    @Override
    public void addConstructor(@NotNull ConstructorDescriptor constructorDescriptor, @NotNull BindingTrace trace) {
        super.addConstructor(constructorDescriptor, trace);
        if (constructorDescriptor.isPrimary()) {
            for (ValueParameterDescriptor valueParameterDescriptor : constructorDescriptor.getValueParameters()) {
                JetParameter parameter = (JetParameter) trace.getBindingContext().get(BindingContext.DESCRIPTOR_TO_DECLARATION, valueParameterDescriptor);
                assert parameter != null;
                if (parameter.getValOrVarNode() == null || !constructorDescriptor.isPrimary()) {
                    scopeForInitializers.addVariableDescriptor(valueParameterDescriptor);
                }
            }
        }
    }

    @Override
    public void addPropertyDescriptor(@NotNull PropertyDescriptor propertyDescriptor) {
        super.addPropertyDescriptor(propertyDescriptor);
        properties.add(propertyDescriptor);
        callableMembers.add(propertyDescriptor);
        scopeForMemberResolution.addPropertyDescriptor(propertyDescriptor);
    }

    @Override
    public void addFunctionDescriptor(@NotNull NamedFunctionDescriptor functionDescriptor) {
        super.addFunctionDescriptor(functionDescriptor);
        functions.add(functionDescriptor);
        callableMembers.add(functionDescriptor);
        scopeForMemberResolution.addFunctionDescriptor(functionDescriptor);
    }

    @NotNull
    public Set<NamedFunctionDescriptor> getFunctions() {
        return functions;
    }

    @NotNull
    public Set<PropertyDescriptor> getProperties() {
        return properties;
    }

    @NotNull
    public Set<CallableMemberDescriptor> getCallableMembers() {
        return callableMembers;
    }

    @Override
    public void addClassifierDescriptor(@NotNull MutableClassDescriptor classDescriptor) {
        super.addClassifierDescriptor(classDescriptor);
        scopeForMemberResolution.addClassifierDescriptor(classDescriptor);
    }

    public void setTypeParameterDescriptors(List<TypeParameterDescriptor> typeParameters) {
        super.setTypeParameterDescriptors(typeParameters);
        for (TypeParameterDescriptor typeParameterDescriptor : typeParameters) {
            scopeForSupertypeResolution.addTypeParameterDescriptor(typeParameterDescriptor);
        }
        scopeForSupertypeResolution.changeLockLevel(WritableScope.LockLevel.READING);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void setName(@NotNull String name) {
        super.setName(name);
        scopeForMemberResolution.addLabeledDeclaration(this);
    }

    @Override
    public void createTypeConstructor() {
        super.createTypeConstructor();
        scopeForMemberResolution.setImplicitReceiver(new ClassReceiver(this));
    }

    @NotNull
    public JetScope getScopeForSupertypeResolution() {
        return scopeForSupertypeResolution;
    }

    @NotNull
    public JetScope getScopeForMemberResolution() {
        return scopeForMemberResolution;
    }
    
    @NotNull
    public JetScope getScopeForInitializers() {
        return scopeForInitializers;
    }

    public void lockScopes() {
        super.lockScopes();
        scopeForSupertypeResolution.changeLockLevel(WritableScope.LockLevel.READING);
        scopeForMemberResolution.changeLockLevel(WritableScope.LockLevel.READING);
        scopeForInitializers.changeLockLevel(WritableScope.LockLevel.READING);
    }

}
