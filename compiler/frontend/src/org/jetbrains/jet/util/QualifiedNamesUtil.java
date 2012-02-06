package org.jetbrains.jet.util;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Common methods for working with qualified names strings.
 *
 * @author Nikolay Krasko
 */
public final class QualifiedNamesUtil {

    public static boolean isSubpackageOf(@NotNull final String subpackageName, @NotNull String packageName) {
        return subpackageName.equals(packageName) ||
               (subpackageName.startsWith(packageName) && subpackageName.charAt(packageName.length()) == '.');
    }
    
    public static boolean isShortNameForFQN(@NotNull final String name, @NotNull final String fqn) {
        return fqn.equals(name) ||
               (fqn.endsWith(name) && fqn.charAt(fqn.length() - name.length() - 1) == '.');
    }
    
    public static boolean isOneSegmentFQN(@NotNull final String fqn) {
        if (fqn.isEmpty()) {
            return false;
        }

        return fqn.indexOf('.') < 0;
    }

    @NotNull
    public static String fqnToShortName(@NotNull String fqName) {
        int lastDotIndex = fqName.lastIndexOf('.');

        if (lastDotIndex != -1) {
            return fqName.substring(lastDotIndex + 1, fqName.length());
        }

        return fqName;
    }

    @NotNull
    public static String withoutLastSegment(@NotNull String fqn) {
        final int lastDotIndex = fqn.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fqn.substring(0, lastDotIndex);
        }

        return "";
    }

    public static String combine(@Nullable String first, @NotNull String second) {
        if (StringUtil.isEmpty(first)) return second;
        return first + "." + second;
    }

//    private static String subPackageName(String packageName, String packageFQN) {
//        if (!isInPackage(packageName, packageFQN)) {
//            return null;
//        }
//
//        int nextDotIndex = packageFQN.indexOf('.', (packageName + ".").length());
//
//        if (nextDotIndex != -1) {
//            return packageFQN.substring((packageName + ".").length(), nextDotIndex);
//        }
//
//        return packageFQN.substring((packageName + ".").length());
//    }
}
