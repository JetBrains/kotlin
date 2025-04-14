// IGNORE_FIR
// ISSUE: KT-75844
package myPack

import myPack.MyTarget.*

object MyTarget {
    const val ANNOTATION_CLASS = 0
}

@Target(ANNOTATION_CLASS)
annotation class MyAnno<caret>tation
