package target

import library.JavaEnum
import library.KtData
import library.KtEnum

class Foo {
    val javaEnum = JavaEnum.values()
    val ktEnum = KtEnum.values()
    val ktData = KtData(1).copy()
    val n = KtData(1).component1()
}