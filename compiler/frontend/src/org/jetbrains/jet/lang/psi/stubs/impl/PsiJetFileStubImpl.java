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

package org.jetbrains.jet.lang.psi.stubs.impl;

import com.intellij.psi.PsiClass;
import com.intellij.psi.impl.java.stubs.StubPsiFactory;
import com.intellij.psi.stubs.PsiFileStubImpl;
import com.intellij.util.io.StringRef;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.stubs.PsiJetFileStub;

/**
 * @author Nikolay Krasko
 */
public class PsiJetFileStubImpl extends PsiFileStubImpl<JetFile> implements PsiJetFileStub {

    private final StringRef packageName;
    private StubPsiFactory psiFactory;

    public PsiJetFileStubImpl(JetFile jetFile, StringRef packageName) {
        super(jetFile);

        this.packageName = packageName;
    }

    @Override
    public String getPackageName() {
        return StringRef.toString(packageName);
    }

    @Override
    public StubPsiFactory getPsiFactory() {
        return psiFactory;
    }

    @Override
    public void setPsiFactory(StubPsiFactory factory) {
        psiFactory = factory;
    }

    @Override
    public PsiClass[] getClasses() {
        return new PsiClass[0];  //To change body of implemented methods use File | Settings | File Templates.
    }
}
