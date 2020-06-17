/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import java.io.File
import java.io.Writer

/**
 * create parent directories and file
 * @return null if file already exists
 */
internal fun getNewFileWriter(
    module: Module,
    relativeDir: String,
    fileName: String
): Writer? {
    val contentEntryPath = module.gradleModuleBuilder?.contentEntryPath ?: return null
    if (contentEntryPath.isEmpty()) return null
    val contentRootDir = File(contentEntryPath)
    val modelContentRootDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(contentRootDir) ?: return null

    val dir = VfsUtil.createDirectoryIfMissing(modelContentRootDir, relativeDir) ?: return null
    if (dir.findChild(fileName) != null) return null
    val file = dir.createChildData(null, fileName)
    return file.getOutputStream(null).writer()
}

internal fun addBrowserSupport(module: Module) {
    getNewFileWriter(module, "src/main/resources", "index.html")?.use {
        it.write(
            """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>${module.name}</title>
                    <script src="${module.name}.js"></script>
                </head>
                <body>
                
                </body>
                </html>
            """.trimIndent().trim()
        )
    }


    getNewFileWriter(module, "src/main/kotlin", "main.kt")?.use {
        it.write(
            """
                import kotlin.browser.document
                
                fun main() {
                    document.write("Hello, world!")
                }
            """.trimIndent().trim()
        )
    }
}

internal fun browserConfiguration(): String {
    return """
        |val cssSupport = { task: KotlinWebpack
        |   task.cssSupport.enabled = true
        |}
        |
        |webpackTask {
        |   cssSupport(it)
        |}
        |
        |runTask {
        |   cssSupport(it)
        |}
    """.trimMargin()
}