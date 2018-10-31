/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.junit.runner.Description
import org.junit.runner.manipulation.Filter
import org.junit.runners.Suite
import org.junit.runners.model.RunnerBuilder
import java.io.File
import java.lang.reflect.Modifier
import java.util.*

annotation class RunOnlyJdk6Test

class SuiteRunnerForCustomJdk constructor(klass: Class<*>, builder: RunnerBuilder?) :
    Suite(builder, klass, getAnnotatedClasses(klass).flatMap {
        collectDeclaredClasses(
            it,
            true
        )
    }.distinct().toTypedArray()) {

    init {
        if (klass.getAnnotation(RunOnlyJdk6Test::class.java) != null) {
            filter(object : Filter() {
                override fun shouldRun(description: Description): Boolean {
                    if (description.isTest) {
                        val methodAnnotation = description.getAnnotation(TestMetadata::class.java) ?: return true
                        val testClassAnnotation = description.testClass.getAnnotation(TestMetadata::class.java) ?: return true

                        val path = testClassAnnotation.value + "/" + methodAnnotation.value
                        val fileText = File(path).readText()
                        return !InTextDirectivesUtils.isDirectiveDefined(fileText, "// JVM_TARGET:") &&
                                !InTextDirectivesUtils.isDirectiveDefined(fileText, "// SKIP_JDK6")
                    }
                    return true
                }

                override fun describe(): String {
                    return "skipped on JDK 6"
                }
            })
        }
    }

    companion object {

        private fun getAnnotatedClasses(klass: Class<*>, addSuperAnnotations: Boolean = true): List<Class<*>> {
            val annotation = klass.getAnnotation(SuiteClasses::class.java)
            return (annotation?.value?.map { it.java } ?: emptyList()) +
                    if (addSuperAnnotations) getAnnotatedClasses(
                        klass.superclass,
                        false
                    ) else emptyList()
        }

        private fun collectDeclaredClasses(klass: Class<*>, withItself: Boolean): List<Class<*>> {
            val result = ArrayList<Class<*>>()
            if (klass.enclosingClass != null && !Modifier.isStatic(klass.modifiers)) return emptyList()

            if (withItself) {
                result.add(klass)
            }

            for (aClass in klass.declaredClasses) {
                result.addAll(collectDeclaredClasses(aClass, true))
            }

            return result
        }
    }
}
