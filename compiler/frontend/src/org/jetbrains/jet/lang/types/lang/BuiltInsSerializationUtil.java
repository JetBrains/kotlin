package org.jetbrains.jet.lang.types.lang;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.descriptors.serialization.ClassId;
import org.jetbrains.jet.descriptors.serialization.ClassSerializationUtil;
import org.jetbrains.jet.descriptors.serialization.NameTable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;

public class BuiltInsSerializationUtil {

    public static final String BUILT_INS_DIR = "builtins";
    public static final String CLASS_METADATA_FILE_EXTENSION = "kotlin_class";
    public static final String PACKAGE_FILE_NAME = ".kotlin_package";
    public static final String NAME_TABLE_FILE_NAME = ".kotlin_name_table";
    public static final Name CLASS_OBJECT_NAME = Name.identifier("object");

    public static final NameTable.Namer BUILTINS_NAMER = new NameTable.Namer() {
        @NotNull
        @Override
        public Name getClassName(@NotNull ClassDescriptor classDescriptor) {
            return classDescriptor.getKind() == ClassKind.CLASS_OBJECT ? CLASS_OBJECT_NAME : classDescriptor.getName();
        }

        @NotNull
        @Override
        public Name getPackageName(@NotNull NamespaceDescriptor namespaceDescriptor) {
            return namespaceDescriptor.getName();
        }
    };

    @NotNull
    public static String getClassMetadataPath(@NotNull ClassId classId) {
        return packageFqNameToPath(classId.getPackageFqName().toUnsafe())
               + "/" + classId.getRelativeClassName().asString()
               + "." + CLASS_METADATA_FILE_EXTENSION;
    }

    @NotNull
    public static ClassId getClassId(@NotNull ClassDescriptor classDescriptor) {
        return ClassSerializationUtil.getClassId(classDescriptor, BUILTINS_NAMER);
    }

    @NotNull
    public static String getPackageFilePath(@NotNull NamespaceDescriptor packageDescriptor) {
        FqNameUnsafe fqName = DescriptorUtils.getFQName(packageDescriptor);
        return packageFqNameToPath(fqName) + "/" + PACKAGE_FILE_NAME;
    }

    @NotNull
    public static String getNameTableFilePath(@NotNull NamespaceDescriptor packageDescriptor) {
        FqNameUnsafe fqName = DescriptorUtils.getFQName(packageDescriptor);
        return packageFqNameToPath(fqName) + "/" + NAME_TABLE_FILE_NAME;
    }

    private static String packageFqNameToPath(FqNameUnsafe fqName) {
        return fqName.asString().replace('.', '/');
    }
}
