/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package idea

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Transformer
import org.gradle.api.file.ContentFilterable
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.RelativePath
import java.io.File
import java.io.FilterReader
import java.io.InputStream
import java.io.OutputStream

class DistCopyDetailsMock(val context: DistModelBuildContext, srcName: String) : FileCopyDetails {
    private var relativePath = RelativePath(true, srcName)
    lateinit var lastAction: Action<in FileCopyDetails>

    class E : Exception() {
        // skip stack trace filling
        override fun fillInStackTrace(): Throwable = this
    }

    fun logUnsupported(methodName: String): Nothing {
        context.logUnsupported("COPY ACTION FileCopyDetails mock: $methodName", lastAction)
        throw E()
    }

    override fun setDuplicatesStrategy(strategy: DuplicatesStrategy) {
        logUnsupported("setDuplicatesStrategy")
    }

    override fun getSourcePath(): String {
        logUnsupported("getSourcePath")
    }

    override fun getName(): String {
        logUnsupported("getName")
    }

    override fun getSize(): Long {
        logUnsupported("getSize")
    }

    override fun getRelativePath(): RelativePath = relativePath

    override fun getRelativeSourcePath(): RelativePath {
        logUnsupported("getRelativeSourcePath")
    }

    override fun expand(properties: MutableMap<String, *>): ContentFilterable {
        logUnsupported("expand")
    }

    override fun getMode(): Int {
        logUnsupported("getMode")
    }

    override fun getSourceName(): String {
        logUnsupported("getSourceName")
    }

    override fun filter(properties: MutableMap<String, *>, filterType: Class<out FilterReader>): ContentFilterable {
        logUnsupported("filter")
    }

    override fun filter(filterType: Class<out FilterReader>): ContentFilterable {
        logUnsupported("filter")
    }

    override fun filter(closure: Closure<*>): ContentFilterable {
        logUnsupported("filter")
    }

    override fun filter(transformer: Transformer<String, String>): ContentFilterable {
        logUnsupported("filter")
    }

    override fun getFile(): File {
        logUnsupported("getFile")
    }

    override fun setMode(mode: Int) {
        logUnsupported("setMode")
    }

    override fun copyTo(output: OutputStream) {
        logUnsupported("copyTo")
    }

    override fun copyTo(target: File): Boolean {
        logUnsupported("copyTo")
    }

    override fun open(): InputStream {
        logUnsupported("open")
    }

    override fun setRelativePath(path: RelativePath) {
        relativePath = path
    }

    override fun getPath(): String {
        logUnsupported("getPath")
    }

    override fun isDirectory(): Boolean {
        logUnsupported("isDirectory")
    }

    override fun getDuplicatesStrategy(): DuplicatesStrategy {
        logUnsupported("getDuplicatesStrategy")
    }

    override fun setName(name: String) {
        logUnsupported("setName")
    }

    override fun getLastModified(): Long {
        logUnsupported("getLastModified")
    }

    override fun setPath(path: String) {
        logUnsupported("setPath")
    }

    override fun exclude() {
        logUnsupported("exclude")
    }

}