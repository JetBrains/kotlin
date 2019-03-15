/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildUtils.idea

class DistModelFlattener() {
    val stack = mutableSetOf<DistVFile>()
    val common = mutableSetOf<DistVFile>()

    fun DistVFile.flatten(): DistVFile {
        val new = DistVFile(parent, name, file)
        copyFlattenedContentsTo(new)
        return new
    }

    private fun DistVFile.copyFlattenedContentsTo(new: DistVFile, inJar: Boolean = false) {
        if (!stack.add(this)) {
            return
        }

        try {
            contents.forEach {
                if (!shouldSkip(new, it)) {
                    when (it) {
                        is DistCopy -> {
                            val srcName = it.customTargetName ?: it.src.name
                            if (it.src.file.exists()) {
                                DistCopy(new, it.src, srcName)
                            }

                            if (!inJar && srcName.endsWith(".jar")) {
                                val newChild = new.getOrCreateChild(srcName)
                                it.src.copyFlattenedContentsTo(newChild, inJar = true)
                            } else {
                                it.src.copyFlattenedContentsTo(new, inJar)
                            }
                        }
                        is DistModuleOutput -> DistModuleOutput(new, it.projectId)
                    }
                }
            }

            child.values.forEach { oldChild ->
                if (inJar) {
                    val newChild =
                        if (oldChild.name.endsWith(".jar")) new
                        else new.getOrCreateChild(oldChild.name)
                    oldChild.copyFlattenedContentsTo(newChild, inJar = true)
                } else {
                    val newChild = new.getOrCreateChild(oldChild.name)
                    oldChild.copyFlattenedContentsTo(newChild)
                }
            }
        } finally {
            stack.remove(this)
        }
    }

    private fun shouldSkip(
        new: DistVFile,
        content: DistContentElement
    ) =
        new.name == "kotlin-jps-plugin.jar" && content is DistCopy && content.customTargetName == "kotlin-compiler-runner.jar"
}

