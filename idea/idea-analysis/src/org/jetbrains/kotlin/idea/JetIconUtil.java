/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea;

import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class JetIconUtil {
    @NotNull
    public static Icon classIcon(boolean locked) {
        return locked ? JetIcons.CLASS_NW : JetIcons.CLASS;
    }

    @NotNull
    public static Icon fileIcon(boolean locked) {
        return locked ? JetIcons.FILE_NW : JetIcons.FILE;
    }

    @NotNull
    public static Icon traitIcon(boolean locked) {
        return locked ? JetIcons.TRAIT_NW : JetIcons.TRAIT;
    }

    @NotNull
    public static Icon enumIcon(boolean locked) {
        return locked ? JetIcons.ENUM_NW : JetIcons.ENUM;
    }

    @NotNull
    public static Icon objectIcon(boolean locked) {
        return locked ? JetIcons.OBJECT_NW : JetIcons.OBJECT;
    }

    public static boolean isLocked(@Nullable PsiElement declaration, @Iconable.IconFlags int flags) {
        return (flags & Iconable.ICON_FLAG_READ_STATUS) != 0 && declaration != null && !declaration.isWritable();
    }
}
