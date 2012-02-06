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
    public static String fqnToShortName(@NotNull String fqn) {
        return getLastSegment(fqn);
    }

    @NotNull
    public static String getLastSegment(@NotNull String fqn) {
        int lastDotIndex = fqn.lastIndexOf('.');
        return (lastDotIndex != -1) ? fqn.substring(lastDotIndex + 1, fqn.length()) : fqn;
    }

    @NotNull
    public static String getFirstSegment(@NotNull String fqn) {
        int dotIndex = fqn.indexOf('.');
        return (dotIndex != -1) ? fqn.substring(0, dotIndex) : fqn;
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

    /**
     * Get tail part of the full fqn by subtracting head part.
     *
     * @param headFQN
     * @param fullFQN
     * @return tail fqn. If first part is not a begging of the full fqn, fullFQN will be returned.
     */
    @NotNull
    public static String tail(@NotNull String headFQN, @NotNull String fullFQN) {
        if (!isSubpackageOf(fullFQN, headFQN)) {
            return fullFQN;
        }

        return fullFQN.equals(headFQN) ?
               "" :
               fullFQN.substring(headFQN.length() + 1); // (headFQN + '.').length
    }

    /**
     * Add one segment of nesting to given qualified name according to the full qualified name.
     *
     * @param fqn
     * @param fullFQN
     * @return qualified name with one more segment or null if fqn is not head part of fullFQN or there's no additional segment.
     */
    @Nullable
    public static String plusOneSegment(String fqn, String fullFQN) {
        if (!isSubpackageOf(fullFQN, fqn)) {
            return null;
        }

        final String nextSegment = getFirstSegment(tail(fqn, fullFQN));

        if (isOneSegmentFQN(nextSegment)) {
            return combine(fqn, nextSegment);
        }

        return null;
    }
}