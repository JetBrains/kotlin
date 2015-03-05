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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetNamedFunction

data public class SourceInfo(val source: String, val pathOrCleanFQN: String, val linesInFile: Int) {

    default  object {
        fun createInfo(element: JetElement?, internalClassName: String): SourceInfo {
            assert(element != null) { "Couldn't create source mapper for null element " + internalClassName }
            val lineNumbers = CodegenUtil.getLineNumberForElement(element!!.getContainingFile(), true)
            assert(lineNumbers != null) { "Couldn't extract line count in " + element.getContainingFile() }

            //TODO hack condition for package parts cleaning
            val isTopLevel = element is JetFile || (element is JetNamedFunction && element.getParent() is JetFile)
            val cleanedClassFqName = if (!isTopLevel) internalClassName else internalClassName.substringBefore('$')

            return SourceInfo(element.getContainingJetFile().getName(), cleanedClassFqName, lineNumbers!!)
        }
    }

}

