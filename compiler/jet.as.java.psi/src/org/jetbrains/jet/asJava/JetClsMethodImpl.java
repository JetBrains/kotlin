/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.asJava;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.compiled.ClsMethodImpl;
import com.intellij.psi.impl.java.stubs.PsiMethodStub;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.resolve.java.jetAsJava.JetClsMethod;

public class JetClsMethodImpl extends ClsMethodImpl implements JetClsMethod {
    @NotNull
    private final PsiElement origin;

    public JetClsMethodImpl(@NotNull PsiMethodStub stub, @NotNull PsiElement origin) {
        super(stub);
        this.origin = origin;
    }

    @Override
    public PsiElement getMirror() {
        return origin;
    }

    @NotNull
    @Override
    public PsiElement getNavigationElement() {
        return origin;
    }

    @Override
    public JetDeclaration getOrigin() {
        return (JetDeclaration) origin;
    }

    @Override
    public void delete() throws IncorrectOperationException {
        origin.delete();
    }

    @Override
    public boolean isEquivalentTo(PsiElement another) {
        if (another instanceof JetClsMethod && getOrigin().equals(((JetClsMethod) another).getOrigin())) return true;
        return super.isEquivalentTo(another);
    }
}
