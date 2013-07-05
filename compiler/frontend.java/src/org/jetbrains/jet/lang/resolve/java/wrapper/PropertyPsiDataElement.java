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

package org.jetbrains.jet.lang.resolve.java.wrapper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.TypeSource;

/**
 * Some PSI corresponding to a property: getter, setter or field.
 */
public final class PropertyPsiDataElement {

    @NotNull
    private final PsiMemberWrapper member;

    // for methods
    private final boolean getter;

    @NotNull
    private final TypeSource type;
    @Nullable
    private final TypeSource receiverType;

    public PropertyPsiDataElement(@NotNull PsiFieldWrapper field, @NotNull TypeSource type, @Nullable TypeSource receiverType) {
        this.member = field;
        this.type = type;
        this.receiverType = receiverType;
        this.getter = false;
    }

    @NotNull
    public PsiMemberWrapper getMember() {
        return member;
    }

    @NotNull
    public TypeSource getType() {
        return type;
    }

    @Nullable
    public TypeSource getReceiverType() {
        return receiverType;
    }

    boolean isExtension() {
        return getReceiverType() != null;
    }

    public boolean isGetter() {
        return member instanceof PsiMethodWrapper && getter;
    }

    public boolean isSetter() {
        return member instanceof PsiMethodWrapper && !getter;
    }

    public boolean isField() {
        return member instanceof PsiFieldWrapper;
    }
}
