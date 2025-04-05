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

package org.jetbrains.kotlin.kdoc.parser;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.ElementTypeChecker;
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens;
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName;
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection;
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag;

@SuppressWarnings("WeakerAccess") // Let all static identifiers be public as well as corresponding elements
public class KDocElementTypes {
    static {
        // It forces initializing tokens in strict order that provides possibility to match indexes and static identifiers
        @SuppressWarnings("unused")
        IElementType dependentTokensInit = KDocTokens.KDOC;
    }

    public static final int KDOC_SECTION_INDEX = KDocTokens.MARKDOWN_INLINE_LINK_INDEX + 1;
    public static final int KDOC_TAG_INDEX = KDOC_SECTION_INDEX + 1;
    public static final int KDOC_NAME_INDEX = KDOC_TAG_INDEX + 1;

    public static final KDocElementType KDOC_SECTION = new KDocElementType("KDOC_SECTION", KDocSection.class);
    public static final KDocElementType KDOC_TAG = new KDocElementType("KDOC_TAG", KDocTag.class);
    public static final KDocElementType KDOC_NAME = new KDocElementType("KDOC_NAME", KDocName.class);

    static {
        ElementTypeChecker.checkExplicitStaticIndexesMatchImplicit(KDocElementTypes.class);
    }
}
