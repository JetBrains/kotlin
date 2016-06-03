/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.actions

import com.intellij.ide.fileTemplates.DefaultCreateFromTemplateHandler
import com.intellij.ide.fileTemplates.FileTemplate
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.core.quoteIfNeeded

class KotlinCreateFromTemplateHandler : DefaultCreateFromTemplateHandler() {
    override fun handlesTemplate(template: FileTemplate) = template.isTemplateOfType(KotlinFileType.INSTANCE)

    override fun prepareProperties(props: MutableMap<String, Any>) {
        val packageName = props[FileTemplate.ATTRIBUTE_PACKAGE_NAME] as? String
        if (!packageName.isNullOrEmpty()) {
            props[FileTemplate.ATTRIBUTE_PACKAGE_NAME] = packageName!!
                    .split('.')
                    .map { it.quoteIfNeeded() }
                    .joinToString(".")
        }

        val name = props[FileTemplate.ATTRIBUTE_NAME] as? String
        if (name != null) {
            props[FileTemplate.ATTRIBUTE_NAME] = name.quoteIfNeeded()
        }
    }
}