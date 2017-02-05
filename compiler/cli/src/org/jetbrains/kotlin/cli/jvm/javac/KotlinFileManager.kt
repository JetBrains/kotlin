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

package org.jetbrains.kotlin.cli.jvm.javac

import javax.tools.*

class KotlinFileManager(val standardFileManager: StandardJavaFileManager,
                        val kotlinLightClasses: List<KotlinLightClass>) :
        ForwardingJavaFileManager<JavaFileManager>(standardFileManager) {

    private val kotlinLightClassLoader = KotlinLightClassLoader(ClassLoader.getSystemClassLoader(), kotlinLightClasses)

    override fun getClassLoader(location: JavaFileManager.Location) = kotlinLightClassLoader

    override fun getJavaFileForOutput(location: JavaFileManager.Location,
                                      className: String,
                                      kind: JavaFileObject.Kind,
                                      sibling: FileObject): JavaFileObject? {
        return if (location == StandardLocation.CLASS_PATH && kind == JavaFileObject.Kind.CLASS) {
            kotlinLightClasses
                    .firstOrNull { it.binaryName == className } ?: standardFileManager.getJavaFileForOutput(location, className, kind, sibling)
        } else standardFileManager.getJavaFileForOutput(location, className, kind, sibling)
    }

    override fun inferBinaryName(location: JavaFileManager.Location,
                                 file: JavaFileObject): String {
        val inferredBinaryName = (file as? KotlinLightClass)?.binaryName ?: standardFileManager.inferBinaryName(location, file)
        return inferredBinaryName
    }

    override fun list(location: JavaFileManager.Location,
                      packageName: String,
                      kinds: MutableSet<JavaFileObject.Kind>,
                      recurse: Boolean): Iterable<JavaFileObject> {
        return if (location == StandardLocation.CLASS_PATH && kinds.contains(JavaFileObject.Kind.CLASS)) {
            kotlinLightClasses.filter { it.packageName == packageName }
                    .toMutableList<JavaFileObject>()
                    .apply { addAll(standardFileManager.list(location, packageName, kinds, recurse)) }

        } else standardFileManager.list(location, packageName, kinds, recurse)
    }

}