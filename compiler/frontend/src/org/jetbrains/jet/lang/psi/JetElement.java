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

package org.jetbrains.jet.lang.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetLanguage;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author max
 */
public class JetElement extends ASTWrapperPsiElement {
    public JetElement(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * Comes along with @Nullable to indicate null is only possible if parsing error present
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface IfNotParsed {}

    @NotNull
    @Override
    public Language getLanguage() {
        return JetLanguage.INSTANCE;
    }

    @Override
    public String toString() {
        return getNode().getElementType().toString();
    }

    @Override
    public final void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof JetVisitorVoid) {
            accept((JetVisitorVoid) visitor);
        }
        else {
            visitor.visitElement(this);
        }
    }

    public <D> void acceptChildren(@NotNull JetTreeVisitor<D> visitor, D data) {
        PsiElement child = getFirstChild();
        while (child != null) {
            if (child instanceof JetElement) {
                ((JetElement) child).accept(visitor, data);
            }
            child = child.getNextSibling();
        }
    }

    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitJetElement(this);
    }
    
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitJetElement(this, data);
    }
}
