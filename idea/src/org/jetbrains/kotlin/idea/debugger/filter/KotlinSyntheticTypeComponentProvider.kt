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

package org.jetbrains.kotlin.idea.debugger.filter

import com.intellij.debugger.engine.SyntheticTypeComponentProvider
import com.sun.jdi.Method
import com.sun.jdi.TypeComponent
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe

public class KotlinSyntheticTypeComponentProvider: SyntheticTypeComponentProvider {
    override fun isSynthetic(typeComponent: TypeComponent?): Boolean {
        if (typeComponent !is Method) return false

        val typeName = typeComponent.declaringType().name()
        if (!FqNameUnsafe.isValid(typeName)) return false

        if (PackageClassUtils.isPackageClassFqName(FqName(typeName)))  {
            val lineNumber = typeComponent.location().lineNumber()
            return lineNumber == 1
        }

        return false
    }
}