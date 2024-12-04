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

package org.jetbrains.kotlin.lexer;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinLanguage;

public class KtToken extends IElementType {
    private static final int INVALID_ID = -1;

    public final int tokenId;

    @Deprecated
    public KtToken(@NotNull @NonNls String debugName) {
        this(debugName, INVALID_ID);
    }

    public KtToken(@NotNull @NonNls String debugName, int tokenId) {
        super(debugName, KotlinLanguage.INSTANCE);
        this.tokenId = tokenId;
    }
}
