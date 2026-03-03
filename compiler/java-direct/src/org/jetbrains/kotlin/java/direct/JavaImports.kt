/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.name.FqName

data class JavaImports(
    val simpleImports: Map<String, FqName>,
    val starImports: List<FqName>,
    val packageFqName: FqName = FqName.ROOT
) {
    companion object {
        val EMPTY = JavaImports(emptyMap(), emptyList(), FqName.ROOT)
    }
}

fun extractImports(root: JavaSyntaxNode, source: CharSequence): JavaImports {
    val simpleImports = mutableMapOf<String, FqName>()
    val starImports = mutableListOf<FqName>()
    
    // Extract package name
    val packageStmt = root.findChildByType("PACKAGE_STATEMENT")
    val packageName = packageStmt?.findChildByType("JAVA_CODE_REFERENCE")?.text
    val packageFqName = if (packageName != null) FqName(packageName) else FqName.ROOT
    
    val importList = root.findChildByType("IMPORT_LIST")
    val importStatements = importList?.getChildrenByType("IMPORT_STATEMENT") ?: emptyList()
    
    for (importNode in importStatements) {
        val codeRef = importNode.findChildByType("JAVA_CODE_REFERENCE")
        if (codeRef == null) continue
        
        val hasStar = importNode.children.any { it.type.toString() == "ASTERISK" }
        val fqName = codeRef.text
        
        if (hasStar) {
            starImports.add(FqName(fqName))
        } else {
            val simpleName = fqName.substringAfterLast('.')
            simpleImports[simpleName] = FqName(fqName)
        }
    }
    
    return JavaImports(simpleImports, starImports, packageFqName)
}
