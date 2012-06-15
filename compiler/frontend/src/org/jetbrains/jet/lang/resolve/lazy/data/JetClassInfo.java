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

package org.jetbrains.jet.lang.resolve.lazy.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.List;

/**
 * @author abreslav
 */
public class JetClassInfo extends JetClassOrObjectInfo<JetClass> {

    protected JetClassInfo(@NotNull JetClass classOrObject) {
        super(classOrObject);
    }

    @Override
    public JetClassObject getClassObject() {
        return element.getClassObject();
    }

    @NotNull
    @Override
    public List<JetTypeParameter> getTypeParameters() {
        return element.getTypeParameters();
    }

    @NotNull
    @Override
    public List<? extends JetParameter> getPrimaryConstructorParameters() {
        return element.getPrimaryConstructorParameters();
    }

    @NotNull
    @Override
    public ClassKind getClassKind() {
        if (element instanceof JetEnumEntry) return ClassKind.ENUM_ENTRY;
        if (element.isTrait()) return ClassKind.TRAIT;
        if (element.hasModifier(JetTokens.ANNOTATION_KEYWORD)) return ClassKind.ANNOTATION_CLASS;
        if (element.hasModifier(JetTokens.ENUM_KEYWORD)) return ClassKind.ENUM_CLASS;
        return ClassKind.CLASS;
    }
}
