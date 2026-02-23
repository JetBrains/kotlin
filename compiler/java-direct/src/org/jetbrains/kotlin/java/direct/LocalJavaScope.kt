/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.Name

class LocalJavaScope(
    private val root: JavaSyntaxNode,
    private val source: CharSequence
) {
    private val classMap: Map<Name, JavaClass> by lazy {
        val classes = root.getChildrenByType("CLASS")
        classes.mapNotNull { classNode ->
            val identifier = classNode.findChildByType("IDENTIFIER")?.text ?: return@mapNotNull null
            val name = Name.identifier(identifier)
            val javaClass = JavaClassOverAst(classNode, source)
            name to javaClass
        }.toMap()
    }

    fun findClass(name: Name): JavaClass? = classMap[name]
}
