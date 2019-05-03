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

import kotlin.DeprecationLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtObjectDeclaration;

public class KtClassInfoUtil {

    /**
     * @deprecated use {@link #createClassOrObjectInfo(KtClassOrObject)} instead.
     */
    @Deprecated
    @kotlin.Deprecated(message = "Use createClassOrObjectInfo(KtClassOrObject) instead", level = DeprecationLevel.ERROR)
    @NotNull
    public static KtClassLikeInfo createClassLikeInfo(@NotNull KtClassOrObject classOrObject) {
        return createClassOrObjectInfo(classOrObject);
    }

    @NotNull
    public static KtClassOrObjectInfo<? extends KtClassOrObject> createClassOrObjectInfo(@NotNull KtClassOrObject classOrObject) {
        if (classOrObject instanceof KtClass) {
            return new KtClassInfo((KtClass) classOrObject);
        }
        if (classOrObject instanceof KtObjectDeclaration) {
            return new KtObjectInfo((KtObjectDeclaration) classOrObject);
        }
        throw new IllegalArgumentException("Unknown declaration type: " + classOrObject + classOrObject.getText());
    }
}
