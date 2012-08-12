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

package org.jetbrains.jet.lang.resolve;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.psi.impl.compiled.ClsMethodImpl;
import com.intellij.psi.impl.java.stubs.JavaClassElementType;
import com.intellij.psi.impl.java.stubs.impl.PsiClassStubImpl;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.psi.impl.source.tree.java.MethodElement;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.stubs.PsiFileStubImpl;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lang.psi.stubs.impl.PsiJetClassStubImpl;
import org.jetbrains.jet.lang.psi.stubs.impl.PsiJetFunctionStubImpl;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lexer.JetToken;

import javax.swing.*;
import java.util.ArrayList;

/**
 * @author Kirill Berezin
 */
public final class DeclarationUtils {

    @Nullable
    public static JetClass toJetDeclaration(@NotNull ClsClassImpl classImpl) {
        PsiClassType[] types = classImpl.getSuperTypes();
        ArrayList<String> superNames = new ArrayList<String>(types.length);
        for (PsiClassType type : types) {
            superNames.add(type.getClassName());
        }
        FileViewProvider provider = ((ClsFileImpl) classImpl.getParent()).getViewProvider();
        //preparing "adapter"
        return new JetClass(new PsiJetClassStubImpl(JetStubElementTypes.CLASS, new PsiFileStubImpl<PsiJavaFileImpl>(new
            PsiJavaFileImpl(provider)), classImpl.getQualifiedName(), classImpl.getName(), superNames, classImpl.isInterface(), classImpl.isEnum(), classImpl.isAnnotationType())) {
            @Override
            public JetParameterList getPrimaryConstructorParameterList() {
                return null; //since interfaces don't have constructors with parameters
            }
        };
    }

    @Nullable
    public static JetNamedFunction toJetDeclaration(@NotNull final ClsMethodImpl method) {
        return new JetNamedFunction(new PsiJetFunctionStubImpl(JetStubElementTypes.FUNCTION, ((ClsClassImpl) method.getParent()).getStub(), method.getName(), false, null, false)) {
            @Override
            public JetModifierList getModifierList() {
                return new JetModifierList(new MethodElement()); //TODO incorrect
            }
        };
    }
}
