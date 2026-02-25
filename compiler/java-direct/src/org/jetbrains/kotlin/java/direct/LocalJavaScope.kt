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
        val result = mutableMapOf<Name, JavaClass>()
        
        fun collectClasses(node: JavaSyntaxNode, outerClass: JavaClass?) {
            for (child in node.children) {
                if (child.type.toString() == "CLASS") {
                    val identifier = child.findChildByType("IDENTIFIER")?.text
                    if (identifier != null) {
                        val name = Name.identifier(identifier)
                        val javaClass = JavaClassOverAst(child, source, outerClass, this, JavaImports.EMPTY)
                        // For now, index by simple name to support simple name resolution within the file.
                        // If there are multiple classes with the same simple name (e.g. in different methods, 
                        // but Java only allows one top-level class with a given name in a file, 
                        // and nested classes have different paths), we might need a better indexing.
                        // But for now, simple name resolution is what's requested.
                        if (!result.containsKey(name)) {
                            result[name] = javaClass
                        }
                        collectClasses(child, javaClass)
                    }
                } else {
                    collectClasses(child, outerClass)
                }
            }
        }
        
        collectClasses(root, null)
        result
    }

    fun findClass(name: Name): JavaClass? = classMap[name]
}
