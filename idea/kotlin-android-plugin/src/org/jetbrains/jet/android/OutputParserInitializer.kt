/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.android

import com.intellij.openapi.components.ProjectComponent
import java.lang.reflect.Modifier
import java.lang.reflect.Field

// TODO drop this when Android Studio will be based on IDEA 14
public fun initializeOutputParser() {
    try {
        val klass = Class.forName("com.android.tools.idea.gradle.output.parser.BuildOutputParser")
        val field = klass.getDeclaredField("PARSERS")
        field.setAccessible(true)

        val modifiersField = javaClass<Field>().getDeclaredField("modifiers")
        modifiersField.setAccessible(true)
        modifiersField.setInt(field, field.getModifiers() and Modifier.FINAL.inv())

        [suppress("UNCHECKED_CAST")]
        val patternAwareClass = Class.forName("com.android.tools.idea.gradle.output.parser.PatternAwareOutputParser") as Class<out Any>

        val array = field.get(null) as Array<*>
        val arrayTypeInstance = java.lang.reflect.Array.newInstance(patternAwareClass, array.size + 1)
        for ((i, item) in array.withIndices()) {
            java.lang.reflect.Array.set(arrayTypeInstance, i, item)
        }
        java.lang.reflect.Array.set(arrayTypeInstance, array.size, KotlinOutputParser())
        field.set(null, arrayTypeInstance)
    }
    catch (e: ClassNotFoundException) {
        // skip: Android plugin doesn't exist
    }
}

public class OutputParserInitializer : ProjectComponent {
    override fun projectOpened() {
    }

    override fun projectClosed() {
    }

    override fun initComponent() {
        initializeOutputParser()
    }

    override fun disposeComponent() {
    }

    override fun getComponentName() = "org.jetbrains.jet.android.OutputParserInitializer"
}
