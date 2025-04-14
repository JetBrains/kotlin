package myPack

import myPack.MyTarget.ANNOTATION_CLASS

enum class MyTarget {
    ANNOTATION_CLASS;
}

@Target(ANNOTATION_CLASS)
annotation class MyAnnot<caret>ation
