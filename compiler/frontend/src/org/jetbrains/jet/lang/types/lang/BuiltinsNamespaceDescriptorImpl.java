package org.jetbrains.jet.lang.types.lang;

import com.intellij.openapi.util.Computable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.*;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedPackageMemberScope;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.AbstractNamespaceDescriptorImpl;
import org.jetbrains.jet.lang.resolve.lazy.storage.NotNullLazyValue;
import org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationDeserializer.UNSUPPORTED;

class BuiltinsNamespaceDescriptorImpl extends AbstractNamespaceDescriptorImpl {
    private final DeserializedPackageMemberScope members;
    private final NameResolver nameResolver;

    public BuiltinsNamespaceDescriptorImpl(@NotNull StorageManager storageManager, @NotNull NamespaceDescriptor containingDeclaration) {
        super(containingDeclaration, Collections.<AnnotationDescriptor>emptyList(), KotlinBuiltIns.BUILT_INS_PACKAGE_NAME);

        nameResolver = NameSerializationUtil.deserializeNameResolver(getStream(BuiltInsSerializationUtil.getNameTableFilePath(this)));

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

    private class BuiltInsDescriptorFinder extends AbstractDescriptorFinder {
        private final NotNullLazyValue<Collection<Name>> classNames;

        public BuiltInsDescriptorFinder(@NotNull StorageManager storageManager) {
            super(storageManager, UNSUPPORTED);

            classNames = storageManager.createLazyValue(new Computable<Collection<Name>>() {
                @Override
                @NotNull
                public Collection<Name> compute() {
                    InputStream in = getStream(BuiltInsSerializationUtil.getClassNamesFilePath(BuiltinsNamespaceDescriptorImpl.this));

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
                Name actualShortName = nameResolver.getName(classProto.getName());
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

        @Nullable
        @Override
        public NamespaceDescriptor findPackage(@NotNull FqName fqName) {
            return fqName.equals(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME) ? BuiltinsNamespaceDescriptorImpl.this : null;
        }

        @NotNull
        @Override
        public Collection<Name> getClassNames(@NotNull FqName packageName) {
            return packageName.equals(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME) ? classNames.compute() : Collections.<Name>emptyList();
        }
    }
}
