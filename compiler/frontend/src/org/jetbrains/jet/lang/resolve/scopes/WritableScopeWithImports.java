package org.jetbrains.jet.lang.resolve.scopes;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public abstract class WritableScopeWithImports extends JetScopeAdapter implements WritableScope {

    private String debugName;

    @Nullable
    private List<JetScope> imports;
    private WritableScope currentIndividualImportScope;
    protected final RedeclarationHandler redeclarationHandler;

    public WritableScopeWithImports(@NotNull JetScope scope, @NotNull RedeclarationHandler redeclarationHandler) {
        super(scope);
        this.redeclarationHandler = redeclarationHandler;
    }



    private LockLevel lockLevel = LockLevel.WRITING;

    @Override
    public void changeLockLevel(LockLevel lockLevel) {
        if (lockLevel.ordinal() < this.lockLevel.ordinal()) {
            throw new IllegalStateException("cannot lower lock level from " + this.lockLevel + " to " + lockLevel);
        }
        this.lockLevel = lockLevel;
    }

    protected void checkMayRead() {
        if (lockLevel != LockLevel.READING && lockLevel != LockLevel.BOTH) {
            throw new IllegalStateException("cannot read with lock level " + lockLevel);
        }
    }

    protected void checkMayWrite() {
        if (lockLevel != LockLevel.WRITING && lockLevel != LockLevel.BOTH) {
            throw new IllegalStateException("cannot write with lock level " + lockLevel);
        }
    }




    public WritableScopeWithImports setDebugName(@NotNull String debugName) {
        checkMayWrite();

        assert this.debugName == null : this.debugName;
        this.debugName = debugName;
        return this;
    }

    @NotNull
    protected final List<JetScope> getImports() {
        if (imports == null) {
            imports = new ArrayList<JetScope>();
        }
        return imports;
    }

    @Override
    public void importScope(@NotNull JetScope imported) {
        checkMayWrite();

        getImports().add(0, imported);
        currentIndividualImportScope = null;
    }

    @Override
    public void getImplicitReceiversHierarchy(@NotNull List<ReceiverDescriptor> result) {
        checkMayRead();

        super.getImplicitReceiversHierarchy(result);
        // Imported scopes come with their receivers
        // Example: class member resolution scope imports a scope of it's class object
        //          members of the class object must be able to find it as an implicit receiver
        for (JetScope scope : getImports()) {
            ReceiverDescriptor definedReceiver = scope.getImplicitReceiver();
            if (definedReceiver.exists()) {
                result.add(0, definedReceiver);
            }
        }
    }

    @Override
    public VariableDescriptor getVariable(@NotNull String name) {
        checkMayRead();

        // Meaningful lookup goes here
        for (JetScope imported : getImports()) {
            VariableDescriptor importedDescriptor = imported.getVariable(name);
            if (importedDescriptor != null) {
                return importedDescriptor;
            }
        }
        return null;
    }

    @NotNull
    @Override
    public Set<FunctionDescriptor> getFunctions(@NotNull String name) {
        checkMayRead();

        if (getImports().isEmpty()) {
            return Collections.emptySet();
        }
        Set<FunctionDescriptor> result = Sets.newLinkedHashSet();
        for (JetScope imported : getImports()) {
            result.addAll(imported.getFunctions(name));
        }
        return result;
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        checkMayRead();

        for (JetScope imported : getImports()) {
            ClassifierDescriptor importedClassifier = imported.getClassifier(name);
            if (importedClassifier != null) {
                return importedClassifier;
            }
        }
        return null;
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        checkMayRead();

        for (JetScope imported : getImports()) {
            NamespaceDescriptor importedDescriptor = imported.getNamespace(name);
            if (importedDescriptor != null) {
                return importedDescriptor;
            }
        }
        return null;
    }

    private WritableScope getCurrentIndividualImportScope() {
        if (currentIndividualImportScope == null) {
            WritableScopeImpl writableScope = new WritableScopeImpl(EMPTY, getContainingDeclaration(), RedeclarationHandler.DO_NOTHING).setDebugName("Individual import scope");
            writableScope.changeLockLevel(LockLevel.BOTH);
            importScope(writableScope);
            currentIndividualImportScope = writableScope;
        }
        return currentIndividualImportScope;
    }

    @Override
    public void importClassifierAlias(@NotNull String importedClassifierName, @NotNull ClassifierDescriptor classifierDescriptor) {
        checkMayWrite();

        getCurrentIndividualImportScope().addClassifierAlias(importedClassifierName, classifierDescriptor);
    }
    
    
    @Override
    public void importNamespaceAlias(String aliasName, NamespaceDescriptor namespaceDescriptor) {
        checkMayWrite();

        getCurrentIndividualImportScope().addNamespaceAlias(aliasName, namespaceDescriptor);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(this)) + " " + debugName + " for " + getContainingDeclaration();
    }

}
