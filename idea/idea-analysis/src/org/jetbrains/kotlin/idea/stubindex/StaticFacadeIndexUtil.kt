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

package org.jetbrains.kotlin.idea.stubindex

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetFile
import kotlin.platform.platformStatic

public object StaticFacadeIndexUtil {

    platformStatic public fun findFilesForStaticFacade(
            facadeFqName: FqName,
            searchScope: GlobalSearchScope,
            project: Project
    ) : Collection<JetFile> =
            JetStaticFacadeClassIndex.INSTANCE.get(facadeFqName.asString(), project, searchScope)

}