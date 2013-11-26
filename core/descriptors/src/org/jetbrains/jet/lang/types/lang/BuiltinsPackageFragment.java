package org.jetbrains.jet.lang.types.lang;

import jet.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.*;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedPackageMemberScope;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.DeclarationDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.MutablePackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.storage.NotNullLazyValue;
import org.jetbrains.jet.storage.StorageManager;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationDeserializer.UNSUPPORTED;

class BuiltinsPackageFragment extends DeclarationDescriptorImpl implements PackageFragmentDescriptor {
    private final DeserializedPackageMemberScope members;
    private final NameResolver nameResolver;
    private final ModuleDescriptor module;
    final PackageFragmentProvider packageFragmentProvider;

    public BuiltinsPackageFragment(@NotNull StorageManager storageManager, @NotNull ModuleDescriptor module) {
        super(Collections.<AnnotationDescriptor>emptyList(), KotlinBuiltIns.BUILT_INS_PACKAGE_NAME);
        this.module = module;
        nameResolver = NameSerializationUtil.deserializeNameResolver(getStream(BuiltInsSerializationUtil.getNameTableFilePath(this)));

        packageFragmentProvider = new BuiltinsPackageFragmentProvider();

        members = new DeserializedPackageMemberScope(storageManager, this, UNSUPPORTED, new BuiltInsDescriptorFinder(storageManager),
                                                     loadPackage(), nameResolver);
    }

    @NotNull
    private ProtoBuf.Package loadPackage() {
        String packageFilePath = BuiltInsSerializationUtil.getPackageFilePath(this);
        InputStream stream = getStream(packageFilePath);
        try {
            return ProtoBuf.Package.parseFrom(stream);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @NotNull
    @Override
    public JetScope getMemberScope() {
        return members;
    }

    @NotNull
    @Override
    public ModuleDescriptor getContainingDeclaration() {
        return module;
    }

    @Nullable
    @Override
    public DeclarationDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        return this;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitPackageFragmentDescriptor(this, data);
    }

    @NotNull
    @Override
    public FqName getFqName() {
        return KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME;
    }

    @NotNull
    private static InputStream getStream(@NotNull String path) {
        InputStream stream = getStreamNullable(path);
        if (stream == null) {
            throw new IllegalStateException("Resource not found in classpath: " + path);
        }
        return stream;
    }

    @Nullable
    private static InputStream getStreamNullable(@NotNull String path) {
        return KotlinBuiltIns.class.getClassLoader().getResourceAsStream(path);
    }

    private class BuiltinsPackageFragmentProvider implements PackageFragmentProvider {
        private final PackageFragmentDescriptor rootPackage = new MutablePackageFragmentDescriptor(module, FqName.ROOT);

        @NotNull
        @Override
        public List<PackageFragmentDescriptor> getPackageFragments(@NotNull FqName fqName) {
            if (fqName.isRoot()) {
                return Collections.singletonList(rootPackage);
            }
            else if (KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME.equals(fqName)) {
                return Collections.<PackageFragmentDescriptor>singletonList(BuiltinsPackageFragment.this);
            }
            return Collections.emptyList();
        }

        @NotNull
        @Override
        public Collection<FqName> getSubPackagesOf(@NotNull FqName fqName) {
            if (fqName.isRoot()) {
                return Collections.singleton(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME);
            }
            return Collections.emptyList();
        }
    }

    private class BuiltInsDescriptorFinder extends AbstractDescriptorFinder {
        private final NotNullLazyValue<Collection<Name>> classNames;

        public BuiltInsDescriptorFinder(@NotNull StorageManager storageManager) {
            super(storageManager, UNSUPPORTED, packageFragmentProvider);

            classNames = storageManager.createLazyValue(new Function0<Collection<Name>>() {
                @Override
                @NotNull
                public Collection<Name> invoke() {
                    InputStream in = getStream(BuiltInsSerializationUtil.getClassNamesFilePath(BuiltinsPackageFragment.this));

                    try {
                        DataInputStream data = new DataInputStream(in);
                        try {
                            int size = data.readInt();
                            List<Name> result = new ArrayList<Name>(size);
                            for (int i = 0; i < size; i++) {
                                result.add(nameResolver.getName(data.readInt()));
                            }
                            return result;
                        }
                        finally {
                            data.close();
                        }
                    }
                    catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            });
        }

        @Nullable
        @Override
        protected ClassData getClassData(@NotNull ClassId classId) {
            InputStream stream = getStreamNullable(BuiltInsSerializationUtil.getClassMetadataPath(classId));
            if (stream == null) {
                return null;
            }

            try {
                ProtoBuf.Class classProto = ProtoBuf.Class.parseFrom(stream);

                Name expectedShortName = classId.getRelativeClassName().shortName();
                Name actualShortName = nameResolver.getClassId(classProto.getFqName()).getRelativeClassName().shortName();
                if (!actualShortName.isSpecial() && !actualShortName.equals(expectedShortName)) {
                    // Workaround for case-insensitive file systems,
                    // otherwise we'd find "Collection" for "collection" etc
                    return null;
                }

                return new ClassData(nameResolver, classProto);
            }
            catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @NotNull
        @Override
        public Collection<Name> getClassNames(@NotNull FqName packageName) {
            return packageName.equals(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME) ? classNames.invoke() : Collections.<Name>emptyList();
        }
    }
}
