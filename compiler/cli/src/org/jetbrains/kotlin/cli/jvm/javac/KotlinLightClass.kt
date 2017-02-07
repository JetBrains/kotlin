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

import org.jetbrains.kotlin.psi.KtFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject

class KotlinLightClass(val binaryName: String,
                       val packageName: String,
                       val ktFile: KtFile,
                       val compiler: JavaAgainstKotlinCompiler) : SimpleJavaFileObject(URI(binaryName), JavaFileObject.Kind.CLASS) {

    val byteCode: ByteArray
        get() = compiler.getByteCode(this)

    override fun openOutputStream() = ByteArrayOutputStream()

    override fun openInputStream() = ByteArrayInputStream(byteCode)

    override fun getName() = binaryName

    override fun isNameCompatible(simpleName: String, kind: JavaFileObject.Kind): Boolean {
        val baseName = simpleName + kind.extension
        return kind == getKind() && (baseName == name || name.endsWith("/$baseName"))
    }

}