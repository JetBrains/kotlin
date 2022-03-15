/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import java.io.File

class SrcFileArtifact(val srcFilePath: String, astArtifactFilePath: String, astBinaryData: ByteArray?) {
    class Artifact(private val artifactFilePath: String, private var binaryData: ByteArray?) {
        fun fetchBinaryAst(): ByteArray? {
            if (binaryData == null) {
                binaryData = File(artifactFilePath).ifExists { readBytes() }
            }
            return binaryData
        }
    }

    val astFileArtifact = Artifact(astArtifactFilePath, astBinaryData)
}

class KLibArtifact(val moduleName: String, val fileArtifacts: List<SrcFileArtifact>)

abstract class ArtifactCache {
    protected val binaryAsts = mutableMapOf<String, ByteArray>()

    fun saveBinaryAst(srcPath: String, astData: ByteArray) {
        binaryAsts[srcPath] = astData
    }

    abstract fun fetchArtifacts(): KLibArtifact
}
