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

package org.jetbrains.kotlin.idea.completion.test

import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.impl.java.stubs.PsiClassStub
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import org.jetbrains.kotlin.asJava.builder.LightClassConstructionContext
import org.jetbrains.kotlin.asJava.builder.StubComputationTracker
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.junit.Assert
import org.picocontainer.MutablePicoContainer
import java.util.*
import java.util.Collections.synchronizedList

object LightClassComputationControl {
    fun testWithControl(project: Project, testText: String, testBody: () -> Unit) {
        val expectedLightClassFqNames = InTextDirectivesUtils.findLinesWithPrefixesRemoved(
                testText, "// $LIGHT_CLASS_DIRECTIVE"
        ).map { it.trim() }

        val actualFqNames = synchronizedList(ArrayList<String>())
        val stubComputationTracker = object : StubComputationTracker {
            override fun onStubComputed(javaFileStub: PsiJavaFileStub, context: LightClassConstructionContext) {
                val qualifiedName = (javaFileStub.childrenStubs.single() as PsiClassStub<*>).qualifiedName!!
                actualFqNames.add(qualifiedName)
            }
        }

        project.withServiceRegistered<StubComputationTracker, Unit>(stubComputationTracker) {
            testBody()
        }

        if (expectedLightClassFqNames.toSortedSet() != synchronized(actualFqNames) { actualFqNames.toSortedSet() }) {
            Assert.fail(
                    "Expected to compute: ${expectedLightClassFqNames.prettyToString()}\n" +
                    "Actually computed: ${actualFqNames.prettyToString()}\n" +
                    "Use $LIGHT_CLASS_DIRECTIVE to specify expected light class computations"
            )
        }
    }

    val LIGHT_CLASS_DIRECTIVE = "LIGHT_CLASS:"

    private fun List<String>.prettyToString() = if (isEmpty()) "<empty>" else joinToString()
}

inline fun <reified T : Any, R> ComponentManager.withServiceRegistered(instance: T, body: () -> R): R {
    val picoContainer = picoContainer as MutablePicoContainer
    val key = T::class.java.name
    try {
        picoContainer.unregisterComponent(key)
        picoContainer.registerComponentInstance(key, instance)
        return body()
    }
    finally {
        picoContainer.unregisterComponent(key)
    }
}
