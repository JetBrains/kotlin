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

package org.jetbrains.kotlin.resolve.lazy.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.ClassKind;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtEnumEntry;
import org.jetbrains.kotlin.psi.KtTypeParameterList;

public class KtClassInfo extends KtClassOrObjectInfo<KtClass> {
    private final ClassKind kind;

    protected KtClassInfo(@NotNull KtClass classOrObject) {
        super(classOrObject);
        if (element instanceof KtEnumEntry) {
            this.kind = ClassKind.ENUM_ENTRY;
        }
        else if (element.isInterface()) {
            this.kind = ClassKind.INTERFACE;
        }
        else if (element.isEnum()) {
            this.kind = ClassKind.ENUM_CLASS;
        }
        else if (element.isAnnotation()) {
            this.kind = ClassKind.ANNOTATION_CLASS;
        }
        else {
            this.kind = ClassKind.CLASS;
        }
    }

    @Nullable
    @Override
    public KtTypeParameterList getTypeParameterList() {
        return element.getTypeParameterList();
    }

    @NotNull
    @Override
    public ClassKind getClassKind() {
        return kind;
    }
}
