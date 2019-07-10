/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.compare

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

class FirVisitorForComparator : FirVisitorVoid() {
    val firTreeNodesClasses: MutableList<String> = mutableListOf()

    override fun visitElement(element: FirElement) {
        if (element !is DummyElement) {
            firTreeNodesClasses += element.javaClass.canonicalName
            element.acceptChildren(this)
        }
    }
}

fun FirFile.areEqualTo(firFile: FirFile): Boolean {
    val thisFirList = FirVisitorForComparator().apply { visitFile(this@areEqualTo) }.firTreeNodesClasses
    val otherFirList = FirVisitorForComparator().apply { visitFile(firFile) }.firTreeNodesClasses

    if (thisFirList.size != otherFirList.size) return false

    for (i in 0 until thisFirList.size) {
        if (thisFirList[i] != otherFirList[i]) {
            return false
        }
    }

    return true
}