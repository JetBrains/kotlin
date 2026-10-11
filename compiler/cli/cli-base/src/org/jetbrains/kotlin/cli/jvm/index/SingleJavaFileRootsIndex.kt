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

import com.intellij.java.syntax.JavaSyntaxDefinition
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import com.intellij.java.syntax.parser.JavaKeywords
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.syntaxElementTypeSetOf
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.PsiPackage
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.SmartList

class SingleJavaFileRootsIndex(private val roots: List<JavaRoot>) {
    init {
        for ((file) in roots) {
            assert(!file.isDirectory) { "Should not be a directory: $file" }
        }
    }

    private var nextRootIndex = 0
    private val classes: MutableMap<ClassId, VirtualFile> = Object2ObjectOpenHashMap()
    private val classesByPackage: MutableMap<FqName, MutableList<ClassId>> = Object2ObjectOpenHashMap()
    private val allPackages: MutableSet<FqName> = ObjectOpenHashSet()

    fun findJavaSourceClass(classId: ClassId): VirtualFile? {
        if (classId.isNestedClass) return null
        classes[classId]?.let { return it }
        while (nextRootIndex < roots.size) {
            indexNextRoot()
            classes[classId]?.let { return it }
        }
        return null
    }

    fun hasPackage(packageFqName: FqName): Boolean {
        if (packageFqName.isRoot) return false
        if (packageFqName in allPackages) return true
        while (nextRootIndex < roots.size) {
            indexNextRoot()
            if (packageFqName in allPackages) return true
        }
        return false
    }

    fun findJavaSourceClasses(packageFqName: FqName): List<ClassId> {
        while (nextRootIndex < roots.size) {
            indexNextRoot()
        }
        return classesByPackage[packageFqName] ?: emptyList()
    }

    private fun indexNextRoot() {
        val root = roots[nextRootIndex++]
        for (classId in JavaSourceClassIdReader(root.file).readClassIds()) {
            classes.putIfAbsent(classId, root.file)
            classesByPackage.getOrPut(classId.packageFqName) { SmartList() }.add(classId)
            var pkg = classId.packageFqName
            while (!pkg.isRoot && allPackages.add(pkg)) {
                pkg = pkg.parent()
            }
        }
    }

    /**
     * Given a .java file, [readClassIds] uses lexer to determine which classes are declared in that file
     */
    @Suppress("UnstableApiUsage")
    private class JavaSourceClassIdReader(file: VirtualFile) {
        private val isPackageInfo = (file.nameWithoutExtension == PsiPackage.PACKAGE_INFO_CLASS)
        private val lexer = JavaSyntaxDefinition.createLexer(LanguageLevel.HIGHEST).apply {
            start(String(file.contentsToByteArray()))
        }
        private var braceBalance = 0
        private var parenthesisBalance = 0

        private fun at(type: SyntaxElementType): Boolean = lexer.getTokenType() == type

        private fun end(): Boolean = lexer.getTokenType() == null

        private fun advance() {
            when {
                at(JavaSyntaxTokenType.LBRACE) -> braceBalance++
                at(JavaSyntaxTokenType.RBRACE) -> braceBalance--
                at(JavaSyntaxTokenType.LPARENTH) -> parenthesisBalance++
                at(JavaSyntaxTokenType.RPARENTH) -> parenthesisBalance--
            }
            lexer.advance()
        }

        private fun tokenText(): String = lexer.getTokenText()

        private fun atClass(): Boolean =
            braceBalance == 0 && parenthesisBalance == 0 && (lexer.getTokenType() in CLASS_KEYWORDS || atRecord())

        private fun atRecord(): Boolean {
            // Note that the soft keyword "record" is lexed as IDENTIFIER instead of RECORD_KEYWORD.
            // This is kind of a sloppy way to parse a soft keyword, but we only do it at the top level, where it seems to work fine.
            return at(JavaSyntaxTokenType.IDENTIFIER) && tokenText() == JavaKeywords.RECORD
        }

        fun readClassIds(): List<ClassId> {
            var packageFqName = FqName.ROOT
            while (!end() && !at(JavaSyntaxTokenType.PACKAGE_KEYWORD) && !atClass()) {
                advance()
            }
            if (at(JavaSyntaxTokenType.PACKAGE_KEYWORD)) {
                val packageName = StringBuilder()
                while (!end() && !at(JavaSyntaxTokenType.SEMICOLON)) {
                    if (at(JavaSyntaxTokenType.IDENTIFIER) || at(JavaSyntaxTokenType.DOT)) {
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
                advance()
                while (!end() && !at(JavaSyntaxTokenType.IDENTIFIER)) {
                    advance()
                }
                if (end()) break
                result.add(ClassId(packageFqName, Name.identifier(tokenText())))
            }

            if (isPackageInfo) {
                result.add(ClassId(packageFqName, PACKAGE_INFO_CLASS_NAME))
            }

            return result
        }

        companion object {
            private val CLASS_KEYWORDS = syntaxElementTypeSetOf(
                JavaSyntaxTokenType.CLASS_KEYWORD, JavaSyntaxTokenType.INTERFACE_KEYWORD, JavaSyntaxTokenType.ENUM_KEYWORD
            )
        }
    }

    companion object {
        internal val PACKAGE_INFO_CLASS_NAME = Name.identifier(PsiPackage.PACKAGE_INFO_CLASS)
    }
}
