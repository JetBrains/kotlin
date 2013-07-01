package org.jetbrains.jet.lang.types.lang;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.descriptors.serialization.ClassId;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.ArrayList;
import java.util.List;

public class BuiltInsSerializationUtil {
    private static final String CLASS_METADATA_FILE_EXTENSION = "kotlin_class";
    private static final String PACKAGE_FILE_NAME = ".kotlin_package";
    private static final String NAME_TABLE_FILE_NAME = ".kotlin_name_table";
    private static final String CLASS_NAMES_FILE_NAME = ".kotlin_class_names";
    private static final String CLASS_OBJECT_NAME = "object";

    private BuiltInsSerializationUtil() {
    }

    @NotNull
    public static String relativeClassNameToFilePath(@NotNull FqNameUnsafe className) {
        List<String> correctedSegments = new ArrayList<String>();
        for (Name segment : className.pathSegments()) {
            if (segment.asString().startsWith("<class-object-for")) {
                correctedSegments.add(CLASS_OBJECT_NAME);
            }
            else {
                assert !segment.isSpecial();
                correctedSegments.add(segment.asString());
            }
        }
        return FqName.fromSegments(correctedSegments).asString();
    }

    @NotNull
    public static String getClassMetadataPath(@NotNull ClassId classId) {
        return packageFqNameToPath(classId.getPackageFqName().toUnsafe())
               + "/" + relativeClassNameToFilePath(classId.getRelativeClassName())
               + "." + CLASS_METADATA_FILE_EXTENSION;
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

    @NotNull
    public static String getClassNamesFilePath(@NotNull NamespaceDescriptor packageDescriptor) {
        FqNameUnsafe fqName = DescriptorUtils.getFQName(packageDescriptor);
        return packageFqNameToPath(fqName) + "/" + CLASS_NAMES_FILE_NAME;
    }

    private static String packageFqNameToPath(FqNameUnsafe fqName) {
        return fqName.asString().replace('.', '/');
    }
}
