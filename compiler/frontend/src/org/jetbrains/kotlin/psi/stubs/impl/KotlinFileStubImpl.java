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

package org.jetbrains.kotlin.psi.stubs.impl;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiClass;
import com.intellij.psi.impl.java.stubs.PsiClassStub;
import com.intellij.psi.stubs.PsiClassHolderFileStub;
import com.intellij.psi.stubs.PsiFileStubImpl;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.IStubFileElementType;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.stubs.KotlinFileStub;
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.kotlin.name.FqName;

import java.util.List;

public class KotlinFileStubImpl extends PsiFileStubImpl<JetFile> implements KotlinFileStub, PsiClassHolderFileStub<JetFile> {

    private final StringRef packageName;
    private final boolean isScript;

    public KotlinFileStubImpl(JetFile jetFile, @NotNull StringRef packageName, boolean isScript) {
        super(jetFile);
        this.packageName = packageName;
        // SCRIPT: PsiJetFileStubImpl knows about scripting
        this.isScript = isScript;
    }

    public KotlinFileStubImpl(JetFile jetFile, @NotNull String packageName, boolean isScript) {
        this(jetFile, StringRef.fromString(packageName), isScript);
    }

    @Override
    @NotNull
    public FqName getPackageFqName() {
        return new FqName(StringRef.toString(packageName));
    }

    @Override
    public boolean isScript() {
        return isScript;
    }

    @Override
    public IStubFileElementType getType() {
        return JetStubElementTypes.FILE;
    }

    @Override
    public String toString() {
        return "PsiJetFileStubImpl[" + "package=" + getPackageFqName().asString() + "]";
    }

    @Override
    public PsiClass[] getClasses() {
        List<PsiClass> result = Lists.newArrayList();
        for (StubElement child : getChildrenStubs()) {
            if (child instanceof PsiClassStub) {
                result.add((PsiClass) child.getPsi());
            }
        }
        return result.toArray(new PsiClass[result.size()]);
    }
}
