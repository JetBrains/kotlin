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

package org.jetbrains.jet.lang.resolve.lazy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;

/**
 * @author abreslav
 */
public class PsiBasedClassMemberDeclarationProvider extends AbstractPsiBasedDeclarationProvider implements ClassMemberDeclarationProvider {

    private final JetClassOrObject classOrObject;
    private JetClassObject jetClassObject;

    public PsiBasedClassMemberDeclarationProvider(@NotNull JetClassOrObject classOrObject) {
        this.classOrObject = classOrObject;
    }

    @NotNull
    @Override
    public JetClassOrObject getOwnerClassOrObject() {
        return classOrObject;
    }

    @Override
    protected void doCreateIndex() {
        for (JetDeclaration declaration : classOrObject.getDeclarations()) {
            if (declaration instanceof JetClassObject) {
                jetClassObject = (JetClassObject) declaration;
            }
            else {
                putToIndex(declaration);
            }
        }

        if (classOrObject instanceof JetClass) {
            JetClass jetClass = (JetClass) classOrObject;
            for (JetParameter parameter : jetClass.getPrimaryConstructorParameters()) {
                if (parameter.getValOrVarNode() != null) {
                    putToIndex(parameter);
                }
            }
        }
    }

    @Override
    public JetClassObject getClassObject() {
        createIndex();
        return jetClassObject;
    }
}
