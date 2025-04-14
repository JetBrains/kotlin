// ISSUE: KT-75844
package myPack

import myPack.MyTarget.*

enum class MyTarget {
    ANNOTATION_CLASS;
}

@Target(ANNOTATION_CLASS)
annotation class MyA<caret>nnotation
