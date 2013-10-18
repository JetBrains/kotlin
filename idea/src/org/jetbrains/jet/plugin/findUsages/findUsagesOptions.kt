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

package org.jetbrains.jet.plugin.findUsages

import com.intellij.find.findUsages.JavaMethodFindUsagesOptions
import com.intellij.openapi.project.Project
import com.intellij.find.findUsages.JavaClassFindUsagesOptions

public class KotlinMethodFindUsagesOptions(project: Project) : JavaMethodFindUsagesOptions(project)

public class KotlinClassFindUsagesOptions(project : Project) : JavaClassFindUsagesOptions(project) {
    public var searchConstructorUsages : Boolean = true

    public override fun equals(o : Any?) : Boolean {
        return super.equals(o) && o is KotlinClassFindUsagesOptions && o.searchConstructorUsages == searchConstructorUsages
    }

    public override fun hashCode() : Int {
        return 31 * super.hashCode() + if (searchConstructorUsages) 1 else 0
    }
}
