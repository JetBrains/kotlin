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

package org.jetbrains.kotlin.arguments

import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*

fun <T : CommonCompilerArguments> mergeBeans(from: CommonCompilerArguments, to: T): T {
    // TODO: rewrite when updated version of com.intellij.util.xmlb is available on TeamCity
    val copy = XmlSerializerUtil.createCopy(to)

    val fromFields = collectFieldsToCopy(from.javaClass)
    for (fromField in fromFields) {
        val toField = copy.javaClass.getField(fromField.name)
        toField.set(copy, fromField.get(from))
    }

    return copy
}

fun collectFieldsToCopy(clazz: Class<*>): List<Field> {
    val fromFields = ArrayList<Field>()

    var currentClass: Class<*>? = clazz
    while (currentClass != null) {
        for (field in currentClass.declaredFields) {
            val modifiers = field.modifiers
            if (!Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)) {
                fromFields.add(field)
            }
        }
        currentClass = currentClass.superclass
    }

    return fromFields
}

fun setupK2JvmArguments(moduleFile: File, settings: K2JVMCompilerArguments) {
    with(settings) {
        module = moduleFile.absolutePath
        noStdlib = true
        noJdk = true
    }
}

fun setupK2JsArguments(_outputFile: File, sourceFiles: Collection<File>, _libraryFiles: List<String>, settings: K2JSCompilerArguments) {
    with(settings) {
        noStdlib = true
        freeArgs = sourceFiles.map { it.path }
        outputFile = _outputFile.path
        metaInfo = true
        libraryFiles = _libraryFiles.toTypedArray()
    }
}
