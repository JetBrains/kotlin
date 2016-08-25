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

package org.jetbrains.kotlin.idea.core

import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.name.FqName
import java.util.*

private val FUNCTION_BODY_TEMPLATE = "New Kotlin Function Body.kt"
private val PROPERTY_INITIALIZER_TEMPLATE = "New Kotlin Property Initializer.kt"
private val SECONDARY_CONSTRUCTOR_BODY_TEMPLATE = "New Kotlin Secondary Constructor Body.kt"
private val ATTRIBUTE_FUNCTION_NAME = "FUNCTION_NAME"
private val ATTRIBUTE_PROPERTY_NAME = "PROPERTY_NAME"

enum class TemplateKind(val templateFileName: String) {
    FUNCTION(FUNCTION_BODY_TEMPLATE),
    SECONDARY_CONSTRUCTOR(SECONDARY_CONSTRUCTOR_BODY_TEMPLATE),
    PROPERTY_INITIALIZER(PROPERTY_INITIALIZER_TEMPLATE)
}

fun getFunctionBodyTextFromTemplate(
        project: Project,
        kind: TemplateKind,
        name: String?,
        returnType: String,
        classFqName: FqName? = null
): String {
    val fileTemplate = FileTemplateManager.getInstance(project)!!.getCodeTemplate(kind.templateFileName)

    val properties = Properties()
    properties.setProperty(FileTemplate.ATTRIBUTE_RETURN_TYPE, returnType)
    if (classFqName != null) {
        properties.setProperty(FileTemplate.ATTRIBUTE_CLASS_NAME, classFqName.asString())
        properties.setProperty(FileTemplate.ATTRIBUTE_SIMPLE_CLASS_NAME, classFqName.shortName().asString())
    }
    if (name != null) {
        val attribute = when (kind) {
            TemplateKind.FUNCTION, TemplateKind.SECONDARY_CONSTRUCTOR -> ATTRIBUTE_FUNCTION_NAME
            TemplateKind.PROPERTY_INITIALIZER -> ATTRIBUTE_PROPERTY_NAME
        }
        properties.setProperty(attribute, name)
    }

    return try {
        fileTemplate!!.getText(properties)
    }
    catch (e: ProcessCanceledException) {
        throw e
    }
    catch (e: Throwable) {
        // TODO: This is dangerous.
        // Is there any way to avoid catching all exceptions?
        throw IncorrectOperationException("Failed to parse file template", e)
    }
}
