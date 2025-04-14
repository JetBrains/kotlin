// IGNORE_FIR
// ISSUE: KT-75844
package myPack

import myPack.MyTarget.ANNOTATION_CLASS

object MyTarget {
    const val ANNOTATION_CLASS = 0
}

@Target(ANNOTATION_CLASS)
annotation class MyAnnot<caret>ation
