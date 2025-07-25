/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.PsiFile;
import com.intellij.psi.stubs.DefaultStubBuilder;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.psiUtil.JvmFileClassInfo;
import org.jetbrains.kotlin.psi.psiUtil.JvmFileClassUtil;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl;

public class KtFileStubBuilder extends DefaultStubBuilder {
    @NotNull
    @Override
    protected StubElement createStubForFile(@NotNull PsiFile file) {
        if (!(file instanceof KtFile)) {
            return super.createStubForFile(file);
        }

        KtFile ktFile = (KtFile) file;
        String packageFqName = ktFile.getPackageFqName().asString();
        boolean isScript = ktFile.isScript();
        if (ktFile.hasTopLevelCallables()) {
            JvmFileClassInfo fileClassInfo = JvmFileClassUtil.getFileClassInfoNoResolve(ktFile);
            String facadeFqNameRef = fileClassInfo.getFacadeClassFqName().asString();
            String partSimpleName = fileClassInfo.getFileClassFqName().shortName().asString();
            return new KotlinFileStubImpl(ktFile, packageFqName, isScript, facadeFqNameRef, partSimpleName, null);
        }

        return new KotlinFileStubImpl(ktFile, packageFqName, isScript, null, null, null);
    }
}
