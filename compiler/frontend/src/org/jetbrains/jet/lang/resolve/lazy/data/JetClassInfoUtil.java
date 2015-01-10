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

package org.jetbrains.jet.lang.resolve.lazy.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.JetClass;
import org.jetbrains.kotlin.psi.JetClassOrObject;
import org.jetbrains.kotlin.psi.JetObjectDeclaration;

public class JetClassInfoUtil {

    @NotNull
    public static JetClassLikeInfo createClassLikeInfo(@NotNull JetClassOrObject classOrObject) {
        if (classOrObject instanceof JetClass) {
            return new JetClassInfo((JetClass) classOrObject);
        }
        if (classOrObject instanceof JetObjectDeclaration) {
            return new JetObjectInfo((JetObjectDeclaration) classOrObject);
        }
        throw new IllegalArgumentException("Unknown declaration type: " + classOrObject + classOrObject.getText());
    }
}
