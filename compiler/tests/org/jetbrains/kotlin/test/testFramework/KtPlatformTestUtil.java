/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.test.testFramework;

import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.ui.Queryable;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// Based on com.intellij.testFramework.PlatformTestUtil
public class KtPlatformTestUtil {
    @NotNull
    public static String getTestName(@NotNull String name, boolean lowercaseFirstLetter) {
        name = StringUtil.trimStart(name, "test");
        return StringUtil.isEmpty(name) ? "" : lowercaseFirstLetter(name, lowercaseFirstLetter);
    }

    @NotNull
    public static String lowercaseFirstLetter(@NotNull String name, boolean lowercaseFirstLetter) {
        if (lowercaseFirstLetter && !isAllUppercaseName(name)) {
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }

    public static boolean isAllUppercaseName(@NotNull String name) {
        int uppercaseChars = 0;
        for (int i = 0; i < name.length(); i++) {
            if (Character.isLowerCase(name.charAt(i))) {
                return false;
            }
            if (Character.isUpperCase(name.charAt(i))) {
                uppercaseChars++;
            }
        }
        return uppercaseChars >= 3;
    }

    @Nullable
    protected static String toString(@Nullable Object node, @Nullable Queryable.PrintInfo printInfo) {
        if (node instanceof AbstractTreeNode) {
            if (printInfo != null) {
                return ((AbstractTreeNode) node).toTestString(printInfo);
            }
            else {
                @SuppressWarnings({"deprecation", "UnnecessaryLocalVariable"}) String presentation =
                        ((AbstractTreeNode) node).getTestPresentation();
                return presentation;
            }
        }
        if (node == null) {
            return "NULL";
        }
        return node.toString();
    }
}