package org.jetbrains.jet.descriptors.serialization.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.*;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.lazy.storage.LockBasedStorageManager;
import org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DeserializedPackageMemberScope extends DeserializedMemberScope {
    @NotNull
    public static DeserializedPackageMemberScope createScopeFromPackageData(
            @NotNull NamespaceDescriptor packageDescriptor,
            @NotNull PackageData packageData,
            @NotNull DescriptorFinder descriptorFinder,
            @NotNull AnnotationDeserializer deserializer,
            @NotNull LockBasedStorageManager storageManager
    ) {
        DescriptorDeserializer descriptorDeserializer = DescriptorDeserializer
                .create(storageManager, packageDescriptor, packageData.getNameResolver(), descriptorFinder, deserializer);
        List<ProtoBuf.Callable> memberList = packageData.getPackageProto().getMemberList();
        return new DeserializedPackageMemberScope(storageManager, packageDescriptor, descriptorDeserializer, memberList, descriptorFinder);
    }

    private final DescriptorFinder descriptorFinder;

    private final FqName packageFqName;

    public DeserializedPackageMemberScope(
            @NotNull StorageManager storageManager,
            @NotNull NamespaceDescriptor packageDescriptor,
            @NotNull DescriptorDeserializer deserializer,
            @NotNull List<ProtoBuf.Callable> membersList,
            @NotNull DescriptorFinder descriptorFinder
    ) {
        super(storageManager, packageDescriptor, deserializer, membersList);
        this.descriptorFinder = descriptorFinder;
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
        ClassDescriptor classDescriptor = descriptorFinder.findClass(new ClassId(packageFqName, FqNameUnsafe.topLevel(name)));
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
        for (Name className : descriptorFinder.getClassNames(packageFqName)) {
            ClassDescriptor classDescriptor = findClassDescriptor(className, object);

            if (classDescriptor != null) {
                assert classDescriptor.getKind().isObject() == object;
                result.add(classDescriptor);
            }
        }
        return result;
    }

    @Override
    protected void addNonDeclaredDescriptors(@NotNull Collection<DeclarationDescriptor> result) {
        // Do nothing
    }

    @Nullable
    @Override
    public NamespaceDescriptor getNamespace(@NotNull Name name) {
        return descriptorFinder.findPackage(packageFqName.child(name));
    }

    @Nullable
    @Override
    protected ReceiverParameterDescriptor getImplicitReceiver() {
        return null;
    }
}
