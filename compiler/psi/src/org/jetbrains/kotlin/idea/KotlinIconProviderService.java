/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.PsiModifierListOwner;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public abstract class KotlinIconProviderService {
    public abstract Icon getFileIcon();
    public abstract Icon getBuiltInFileIcon();

    public abstract Icon getLightVariableIcon(@NotNull PsiModifierListOwner element, int flags);

    public static class CompilerKotlinFileIconProviderService extends KotlinIconProviderService {
        @Override
        public Icon getFileIcon() {
            return null;
        }

        @Override
        public Icon getBuiltInFileIcon() {
            return null;
        }

        @Override
        public Icon getLightVariableIcon(@NotNull PsiModifierListOwner element, int flags) {
            return null;
        }
    }

    public static KotlinIconProviderService getInstance() {
        KotlinIconProviderService service = ApplicationManager.getApplication().getService(KotlinIconProviderService.class);
        return service != null ? service : new CompilerKotlinFileIconProviderService();
    }
}
