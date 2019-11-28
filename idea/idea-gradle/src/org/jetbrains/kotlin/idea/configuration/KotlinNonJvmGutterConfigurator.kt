/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.util.Consumer
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

class KotlinNonJvmGutterConfigurator : AbstractProjectResolverExtension() {
    override fun enhanceTaskProcessing(taskNames: MutableList<String>, jvmParametersSetup: String?, initScriptConsumer: Consumer<String>) {
        initScriptConsumer.consume(
            //language=Gradle
            """
            ({
                def doIfInstance = { Task task, String fqn, Closure action ->
                    def taskSuperClass = task.class
                    while (taskSuperClass != Object.class) {
                        if (taskSuperClass.canonicalName == fqn) {
                            action()

                            return
                        } else {
                            taskSuperClass = taskSuperClass.superclass
                        }
                    }
                }
                
                gradle.afterProject { project ->
                    // Test task should have some parameters
                    // Create dummy task to consume outputs by Test task
                    project.tasks.create("nonJvmTestIdeSupportDummy")
                    
                    // IDEA now process filter parameters only for Test tasks
                    project.tasks.create('nonJvmTestIdeSupport', Test) {
                        testClassesDirs = project.tasks["nonJvmTestIdeSupportDummy"].outputs.files
                        classpath = project.tasks["nonJvmTestIdeSupportDummy"].outputs.files
                    }
                    
                    project.afterEvaluate {
                        project.tasks.each { Task task ->
                            doIfInstance(task, "org.jetbrains.kotlin.gradle.tasks.KotlinTest") {
                                task.dependsOn('nonJvmTestIdeSupport')
                            }
                        }
                    }
                }
                
                gradle.taskGraph.beforeTask { Task task ->
                    doIfInstance(task, "org.jetbrains.kotlin.gradle.tasks.KotlinTest") {
                        task.filter.includePatterns = task.project.tasks['nonJvmTestIdeSupport'].filter.includePatterns
                    }
                }
            })()
            """.trimIndent()
        )
    }
}