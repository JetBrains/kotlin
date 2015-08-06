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

package org.jetbrains.kotlin.idea.search

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.search.SearchScope

public fun SearchScope.and(otherScope: SearchScope): SearchScope = intersectWith(otherScope)
public fun SearchScope.or(otherScope: SearchScope): SearchScope = union(otherScope)
public fun SearchScope.minus(otherScope: GlobalSearchScope): SearchScope = this and !otherScope
public fun GlobalSearchScope.not(): GlobalSearchScope = GlobalSearchScope.notScope(this)

public fun Project.allScope(): GlobalSearchScope = GlobalSearchScope.allScope(this)

public fun Project.projectScope(): GlobalSearchScope = GlobalSearchScope.projectScope(this)

public fun PsiFile.fileScope(): GlobalSearchScope = GlobalSearchScope.fileScope(this)