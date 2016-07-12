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

import com.intellij.openapi.util.text.StringUtil;

public enum KDocKnownTag {
    AUTHOR(false, false),
    THROWS(true, false),
    EXCEPTION(true, false),
    PARAM(true, false),
    RETURN(false, false),
    SEE(true, false),
    SINCE(false, false),
    CONSTRUCTOR(false, true),
    PROPERTY(true, true),
    SAMPLE(true, false),
    SUPPRESS(false, false);

    private final boolean takesReference;
    private final boolean startsSection;

    KDocKnownTag(boolean takesReference, boolean startsSection) {
        this.takesReference = takesReference;
        this.startsSection = startsSection;
    }

    public boolean isReferenceRequired() {
        return takesReference;
    }

    public boolean isSectionStart() {
        return startsSection;
    }

    public static KDocKnownTag findByTagName(CharSequence tagName) {
        if (StringUtil.startsWith(tagName, "@")) {
            tagName = tagName.subSequence(1, tagName.length());
        }
        try {
            return valueOf(tagName.toString().toUpperCase());
        }
        catch (IllegalArgumentException ignored) {
        }
        return null;
    }
}
