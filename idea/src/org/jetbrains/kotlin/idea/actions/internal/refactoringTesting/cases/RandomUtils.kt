/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions.internal.refactoringTesting.cases

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope
import org.apache.commons.lang.RandomStringUtils
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass
import java.io.File
import kotlin.random.Random

internal fun <T> List<T>.randomElement(): T = this[Random.nextInt(0, this.size)]

internal fun <T : Any> List<T>.randomElementOrNull(): T? = if (isNotEmpty()) randomElement() else null

internal fun <T : Any> List<T>.toRandomSequence(): Sequence<T> = generateSequence { randomElementOrNull() }

internal fun <T> List<T>.randomElements(count: Int): List<T> =
    mutableListOf<T>().also { list -> repeat(count) { list.add(randomElement()) } }

internal fun <T> List<T>.randomElements(): List<T> = randomElements(Random.nextInt(0, size))

internal fun getRandomFileClassElements(project: Project, ktFiles: List<VirtualFile>): List<KtClass> {
    val randomSourceFile = ktFiles.randomElement()
    return PsiTreeUtil.collectElementsOfType(randomSourceFile.toPsiFile(project), KtClass::class.java).toList()
}

internal fun randomBoolean() = Random.nextBoolean()

internal fun Project.files(): List<VirtualFile> {
    val scope = KotlinSourceFilterScope.projectSources(ProjectScope.getContentScope(this), this)
    return FileTypeIndex.getFiles(KotlinFileType.INSTANCE, scope).toList()
}

internal inline fun <T> randomNullability(body: () -> T) = randomFunctor(0.8F, body, { null })

internal fun randomString() = RandomStringUtils.randomAlphanumeric(Random.nextInt(1, 10))

internal fun randomClassName(): String {
    var count = Random.nextInt(1, 10)
    val strings = generateSequence {
        count--
        if (count > 0) randomString() else null
    }
    return strings.joinToString(".")
}

inline fun randomAction(probability: Float, body: () -> Unit) {
    if (Random.nextFloat() <= probability) {
        body()
    }
}

inline fun <T> randomFunctor(probability: Float, `then`: () -> T, otherwise: () -> T): T {
    return if (Random.nextFloat() <= probability) {
        then()
    } else {
        otherwise()
    }
}

inline fun randomActionFiftyFifty(body: () -> Unit) {
    if (Random.nextBoolean()) body()
}


private fun mutatePath(path: File): File {

    randomAction(0.2F) {
        return path
    }

    var file = path

    repeat(Random.nextInt(2)) {
        file = file.parentFile
    }

    repeat(Random.nextInt(2)) {
        file = File(file, randomString())
    }

    return file
}

internal fun randomFileNameMutator(fileName: String, preferableExtension: String): String {

    randomAction(0.2F) {
        return fileName
    }

    val extension = randomFunctor(
        0.8F,
        then = { preferableExtension },
        otherwise = { ".${randomString()}" }
    )

    return "${randomString()}$extension"
}

internal fun randomFilePathMutator(filePath: String, preferableExtension: String): String {
    randomAction(0.2F) {
        return filePath
    }

    val file = File(filePath)

    val mutatedDirectory = mutatePath(file.parentFile)

    return File(
        mutatedDirectory,
        randomFileNameMutator(file.name, preferableExtension)
    ).absolutePath
}

internal fun randomDirectoryPathMutator(baseDirectory: String): String {
    return mutatePath(File(baseDirectory)).absolutePath
}