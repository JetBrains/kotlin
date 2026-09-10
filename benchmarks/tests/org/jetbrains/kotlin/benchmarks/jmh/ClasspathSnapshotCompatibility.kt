/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks.jmh

import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotExternalizer
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotter
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.security.MessageDigest

/** Run in separate JVMs with baseline/candidate classes first on the classpath; compare the complete output. */
fun main(args: Array<String>) {
    val entries = args.flatMap { path ->
        val file = File(path)
        if (file.isFile || file.name == "classes") listOf(file)
        else file.walkTopDown().filter { it.isDirectory && it.name == "classes" }.toList()
    }.sortedBy { it.path }
    check(entries.isNotEmpty())
    for (entry in entries) {
        for (granularity in ClassSnapshotGranularity.entries) {
            for (parseInlinedLocalClasses in listOf(false, true)) {
                for (expandTypeAliases in listOf(false, true)) {
                    val settings = ClasspathEntrySnapshotter.Settings(granularity, parseInlinedLocalClasses, expandTypeAliases)
                    val snapshot = ClasspathEntrySnapshotter.snapshot(entry, settings)
                    val bytes = ByteArrayOutputStream().also { output ->
                        DataOutputStream(output).use { ClasspathEntrySnapshotExternalizer.save(it, snapshot) }
                    }.toByteArray()
                    val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
                    println("${entry.path} $settings ${snapshot.classSnapshots.size} ${bytes.size} $hash")
                }
            }
        }
    }
}
