package target

import library.JavaEnum
import library.KtEnum
import library.KtData

class Foo {
    val javaEnum = JavaEnum.values()
    val ktEnum = KtEnum.values()
    val ktData = KtData(1).copy()
    val n = KtData(1).component1()
}