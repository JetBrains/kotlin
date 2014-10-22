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
}

class Bar {

}