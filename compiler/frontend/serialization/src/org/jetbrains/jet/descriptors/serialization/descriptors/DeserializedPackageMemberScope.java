package org.jetbrains.jet.descriptors.serialization.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.ClassId;
import org.jetbrains.jet.descriptors.serialization.ClassResolver;
import org.jetbrains.jet.descriptors.serialization.DescriptorDeserializer;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class DeserializedPackageMemberScope extends DeserializedMemberScope {

    private final ClassResolver classResolver;
    private final FqName packageFqName;

    public DeserializedPackageMemberScope(
            @NotNull StorageManager storageManager,
            @NotNull NamespaceDescriptor packageDescriptor,
            @NotNull DescriptorDeserializer deserializer,
            @NotNull List<ProtoBuf.Callable> membersList,
            ClassResolver classResolver
    ) {
        super(storageManager, packageDescriptor, deserializer, membersList);
        this.classResolver = classResolver;
        this.packageFqName = DescriptorUtils.getFQName(packageDescriptor).toSafe();
    }

    @Nullable
    @Override
    protected ClassifierDescriptor getClassDescriptor(@NotNull Name name) {
        return findClassDescriptor(name, false);
    }

    @Nullable
    @Override
    public ClassDescriptor getObjectDescriptor(@NotNull Name name) {
        return findClassDescriptor(name, true);
    }

    @Nullable
    private ClassDescriptor findClassDescriptor(Name name, boolean object) {
        ClassDescriptor classDescriptor = classResolver.findClass(new ClassId(packageFqName, FqNameUnsafe.topLevel(name)));
        if (classDescriptor == null) {
            return null;
        }
        return classDescriptor.getKind().isObject() == object ? classDescriptor : null;
    }

    @Override
    protected void addAllClassDescriptors(@NotNull Collection<DeclarationDescriptor> result) {
        findClassifiers(result, false);
    }

    @NotNull
    @Override
    protected Collection<ClassDescriptor> computeAllObjectDescriptors() {
        return findClassifiers(new ArrayList<ClassDescriptor>(), true);
    }

    private <T extends Collection<? super ClassDescriptor>> T findClassifiers(T result, boolean object) {
        for (Name className : getClassNames()) {
            ClassDescriptor classDescriptor = (ClassDescriptor) getClassDescriptor(className);
            assert classDescriptor != null : "Class not found: " + className;

            if (classDescriptor.getKind().isObject() == object) {
                result.add(classDescriptor);
            }
        }
        return result;
    }

    @NotNull
    protected abstract Collection<Name> getClassNames();

    @Override
    protected void addNonDeclaredDescriptors(@NotNull Collection<DeclarationDescriptor> result) {
        // Do nothing
    }
}
