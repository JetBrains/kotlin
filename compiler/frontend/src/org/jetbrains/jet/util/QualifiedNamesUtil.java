/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.FqName;

/**
 * Common methods for working with qualified names strings.
 *
 * @author Nikolay Krasko
 */
public final class QualifiedNamesUtil {

    public static boolean isSubpackageOf(@NotNull final FqName subpackageName, @NotNull FqName packageName) {
        return subpackageName.equals(packageName) ||
               (subpackageName.getFqName().startsWith(packageName.getFqName()) && subpackageName.getFqName().charAt(packageName.getFqName().length()) == '.');
    }

    public static boolean isShortNameForFQN(@NotNull final String name, @NotNull final FqName fqn) {
        return fqn.getFqName().equals(name) ||
               (fqn.getFqName().endsWith(name) && fqn.getFqName().charAt(fqn.getFqName().length() - name.length() - 1) == '.');
    }

    public static boolean isOneSegmentFQN(@NotNull final String fqn) {
        if (fqn.isEmpty()) {
            return false;
        }

        return fqn.indexOf('.') < 0;
    }

    @NotNull
    public static String fqnToShortName(@NotNull FqName fqn) {
        return getLastSegment(fqn);
    }

    @NotNull
    public static String getLastSegment(@NotNull FqName fqn) {
        return fqn.shortName();
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

    @NotNull
    public static FqName withoutLastSegment(@NotNull FqName fqName) {
        return fqName.parent();
    }

    @NotNull
    public static FqName combine(@NotNull FqName first, @NotNull String second) {
        return first.child(second);
    }

    /**
     * Get tail part of the full fqn by subtracting head part.
     *
     * @param headFQN
     * @param fullFQN
     * @return tail fqn. If first part is not a begging of the full fqn, fullFQN will be returned.
     */
    @NotNull
    public static String tail(@NotNull FqName headFQN, @NotNull FqName fullFQN) {
        if (!isSubpackageOf(fullFQN, headFQN)) {
            return fullFQN.getFqName();
        }

        return fullFQN.equals(headFQN) ?
               "" :
               fullFQN.getFqName().substring(headFQN.getFqName().length() + 1); // (headFQN + '.').length
    }

    /**
     * Add one segment of nesting to given qualified name according to the full qualified name.
     *
     * @param fqn
     * @param fullFQN
     * @return qualified name with one more segment or null if fqn is not head part of fullFQN or there's no additional segment.
     */
    @Nullable
    public static FqName plusOneSegment(@NotNull FqName fqn, @NotNull FqName fullFQN) {
        if (!isSubpackageOf(fullFQN, fqn)) {
            return null;
        }

        final String nextSegment = getFirstSegment(tail(fqn, fullFQN));

        if (isOneSegmentFQN(nextSegment)) {
            return combine(fqn, nextSegment);
        }

        return null;
    }

    /**
     * Check that given fqn could be imported with import.
     *
     * @param importPath path from the import. Could contain .* part
     * @param fqn or another import directive
     */
    public static boolean isImported(@NotNull String importPath, @NotNull String fqn) {
        if (importPath.endsWith("*")) {
            // TODO: import path is not valid FQN
            return withoutLastSegment(importPath).equals(withoutLastSegment(fqn));
        }

        return importPath.equals(fqn);
    }
}
