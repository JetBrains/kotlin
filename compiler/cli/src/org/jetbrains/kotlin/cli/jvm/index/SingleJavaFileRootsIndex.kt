/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.jvm.index

import com.intellij.lang.java.lexer.JavaLexer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.impl.source.tree.ElementType
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class SingleJavaFileRootsIndex(private val roots: List<JavaRoot>) {
    init {
        for ((file) in roots) {
            assert(!file.isDirectory) { "Should not be a directory: $file" }
        }
    }

    private val classIdsInRoots = ArrayList<List<ClassId>>(roots.size)

    fun findJavaSourceClass(classId: ClassId): VirtualFile? =
        roots.indices
            .find { index -> classId in getClassIdsForRootAt(index) }
            ?.let { index -> roots[index].file }

    fun findJavaSourceClasses(packageFqName: FqName): List<ClassId> =
        roots.indices.flatMap(this::getClassIdsForRootAt).filter { root -> root.packageFqName == packageFqName }

    private fun getClassIdsForRootAt(index: Int): List<ClassId> {
        for (i in classIdsInRoots.size..index) {
            classIdsInRoots.add(JavaSourceClassIdReader(roots[i].file).readClassIds())
        }
        return classIdsInRoots[index]
    }

    /**
     * Given a .java file, [readClassIds] uses lexer to determine which classes are declared in that file
     */
    private class JavaSourceClassIdReader(file: VirtualFile) {
        private val lexer = JavaLexer(LanguageLevel.JDK_1_9).apply {
            start(String(file.contentsToByteArray()))
        }
        private var braceBalance = 0

        private fun at(type: IElementType): Boolean = lexer.tokenType == type

        private fun end(): Boolean = lexer.tokenType == null

        private fun advance() {
            when {
                at(ElementType.LBRACE) -> braceBalance++
                at(ElementType.RBRACE) -> braceBalance--
            }
            lexer.advance()
        }

        private fun tokenText(): String = lexer.tokenText

        private fun atClass(): Boolean =
            braceBalance == 0 && lexer.tokenType in CLASS_KEYWORDS

        fun readClassIds(): List<ClassId> {
            var packageFqName = FqName.ROOT
            while (!end() && !at(ElementType.PACKAGE_KEYWORD) && !atClass()) {
                advance()
            }
            if (at(ElementType.PACKAGE_KEYWORD)) {
                val packageName = StringBuilder()
                while (!end() && !at(ElementType.SEMICOLON)) {
                    if (at(ElementType.IDENTIFIER) || at(ElementType.DOT)) {
                        packageName.append(tokenText())
                    }
                    advance()
                }
                packageFqName = FqName(packageName.toString())
            }

            val result = ArrayList<ClassId>(1)

            while (true) {
                while (!end() && !atClass()) {
                    advance()
                }
                if (end()) break
                while (!end() && !at(ElementType.IDENTIFIER)) {
                    advance()
                }
                if (end()) break
                result.add(ClassId(packageFqName, Name.identifier(tokenText())))
            }

            return result
        }

        companion object {
            private val CLASS_KEYWORDS = setOf(ElementType.CLASS_KEYWORD, ElementType.INTERFACE_KEYWORD, ElementType.ENUM_KEYWORD)
        }
    }
}
