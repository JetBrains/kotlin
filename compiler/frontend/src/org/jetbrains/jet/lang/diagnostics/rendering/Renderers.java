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

package org.jetbrains.jet.lang.diagnostics.rendering;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.Named;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author svtk
 */
public class Renderers {
    public static final Renderer<Object> TO_STRING = new Renderer<Object>() {
        @NotNull
        @Override
        public String render(@NotNull Object element) {
            return element.toString();
        }

        @Override
        public String toString() {
            return "TO_STRING";
        }
    };

    public static final Renderer<Object> NAME = new Renderer<Object>() {
        @NotNull
        @Override
        public String render(@NotNull Object element) {
            if (element instanceof Named) {
                return ((Named) element).getName();
            }
            return element.toString();
        }
    };

    public static final Renderer<PsiElement> ELEMENT_TEXT = new Renderer<PsiElement>() {
        @NotNull
        @Override
        public String render(@NotNull PsiElement element) {
            return element.getText();
        }
    };
    
    public static final Renderer<JetClassOrObject> RENDER_CLASS_OR_OBJECT = new Renderer<JetClassOrObject>() {
        @NotNull
        @Override
        public String render(@NotNull JetClassOrObject classOrObject) {
            String name = classOrObject.getName() != null ? " '" + classOrObject.getName() + "'" : "";
            if (classOrObject instanceof JetClass) {
                return "Class" + name;
            }
            return "Object" + name;

        }
    };

    public static final Renderer<JetType> RENDER_TYPE = new Renderer<JetType>() {
        @NotNull
        @Override
        public String render(@NotNull JetType type) {
            return DescriptorRenderer.TEXT.renderType(type);
        }
    };

    public static final Renderer<Collection<? extends ResolvedCall<? extends CallableDescriptor>>> AMBIGUOUS_CALLS =
            new Renderer<Collection<? extends ResolvedCall<? extends CallableDescriptor>>>() {
                @NotNull
                @Override
                public String render(@NotNull Collection<? extends ResolvedCall<? extends CallableDescriptor>> argument) {
                    StringBuilder stringBuilder = new StringBuilder("\n");
                    for (ResolvedCall<? extends CallableDescriptor> call : argument) {
                        stringBuilder.append(DescriptorRenderer.TEXT.render(call.getResultingDescriptor())).append("\n");
                    }
                    return stringBuilder.toString();
                }
            };

    public static <T> Renderer<Collection<? extends T>> commaSeparated(final Renderer<T> itemRenderer) {
        return new Renderer<Collection<? extends T>>() {
            @NotNull
            @Override
            public String render(@NotNull Collection<? extends T> object) {
                StringBuilder result = new StringBuilder();
                for (Iterator<? extends T> iterator = object.iterator(); iterator.hasNext(); ) {
                    T next = iterator.next();
                    result.append(itemRenderer.render(next));
                    if (iterator.hasNext()) {
                        result.append(", ");
                    }
                }
                return result.toString();
            }
        };
    }

    private Renderers() {
    }
}
