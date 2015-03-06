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

package org.jetbrains.kotlin.kdoc.psi.api

import com.intellij.psi.PsiComment
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag

// Don't implement JetElement (or it will be treated as statement)
public trait KDoc : PsiComment {
    public fun getOwner(): JetDeclaration?
    public fun getDefaultSection(): KDocSection
    public fun findSectionByName(name: String): KDocSection?
    public fun findSectionByTag(tag: KDocKnownTag): KDocSection?
    public fun findSectionByTag(tag: KDocKnownTag, subjectName: String): KDocSection?
}
