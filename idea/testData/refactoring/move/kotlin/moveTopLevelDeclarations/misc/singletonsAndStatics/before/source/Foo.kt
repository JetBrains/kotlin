package source

import library.*

class <caret>Foo {
    val jv1 = JavaClass.foo()
    val jv2 = JavaClass().foo()
    val jv3 = JavaClass.Inner.foo()
    val jv4 = JavaClass.Inner().foo()
    val kt1 = KtClass.foo()
    val kt2 = KtObject.foo()
    val kt3 = KtClass.Inner.foo()
    val kt4 = KtObject.Inner.foo()
    val kt5 = KtClass.Default.foo()
    val kt6 = KtClass.Default
    val kt7 = KtClass

    val kt8 = Bar
    val kt9 = Bar.Default
    val kt10 = Bar.Default.c
}

class Bar {
    default object {
        val c : Int
    }
}