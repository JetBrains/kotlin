/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.diagnostics;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.Named;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetClassOrObject;

/**
 * @author svtk
 */
public class Renderers {
    public static final Renderer<Object> TO_STRING = new Renderer<Object>() {
        @NotNull
        @Override
        public String render(@Nullable Object element) {
            return element == null ? "null" : element.toString();
        }

        @Override
        public String toString() {
            return "TO_STRING";
        }
    };

    public static final Renderer<Object> NAME = new Renderer<Object>() {
        @NotNull
        @Override
        public String render(@Nullable Object element) {
            if (element == null) return "null";
            if (element instanceof Named) {
                return ((Named) element).getName();
            }
            return element.toString();
        }
    };

    public static final Renderer<PsiElement> ELEMENT_TEXT = new Renderer<PsiElement>() {
        @NotNull
        @Override
        public String render(@Nullable PsiElement element) {
            if (element == null) return "null";
            return element.getText();
        }
    };
    
    public static final Renderer<JetClassOrObject> RENDER_CLASS_OR_OBJECT = new Renderer<JetClassOrObject>() {
        @NotNull
        @Override
        public String render(@Nullable JetClassOrObject classOrObject) {
            assert classOrObject != null;
            String name = classOrObject.getName() != null ? " '" + classOrObject.getName() + "'" : "";
            if (classOrObject instanceof JetClass) {
                return "Class" + name;
            }
            return "Object" + name;

        }
    };
}
