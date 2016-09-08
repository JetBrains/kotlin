package org.kotlinnative.translator.debug

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.CompositeElement
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

fun debugPrintNode(node: Any?, indent: Int = 0): Unit {
    if (node == null) {
        return
    }

    for (i in 0..indent) {
        print(" ")
    }

    println(node.toString())

    when (node) {
        is CompositeElement -> {
            debugPrintNode(node.firstChildNode, indent + 2)
            debugPrintNode(node.treeNext, indent)
        }
        is PsiElement -> {
            debugPrintNode(node.firstChild, indent + 2)
            debugPrintNode(node.nextSibling, indent)
        }
        is PsiWhiteSpace -> {
            debugPrintNode(node.firstChild, indent + 2)
            debugPrintNode(node.nextSibling, indent)
        }
        else -> {
            print("  not supported: " + node.javaClass.canonicalName)
        }
    }

}

fun printFunction(function: KtNamedFunction) {
    debugPrintNode(function.node)
}

fun printClass(function: KtClass) {
    debugPrintNode(function.node)
}

fun printFile(file: KtFile) {

    for (declaration in file.declarations) {

        when (declaration) {
            is KtNamedFunction -> printFunction(declaration)
            is KtClass -> printClass(declaration)
            else -> println(declaration.toString())
        }
    }
}