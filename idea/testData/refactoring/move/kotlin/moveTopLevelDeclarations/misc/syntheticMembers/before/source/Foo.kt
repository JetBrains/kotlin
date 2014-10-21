package source

import library.*

class <caret>Foo {
    val javaEnum = JavaEnum.values()
    val ktEnum = KtEnum.values()
    val ktData = KtData(1).copy()
    val n = KtData(1).component1()
}

class Bar {

}