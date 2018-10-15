package common

actual class A {
    fun id1() {}
}


fun use() {
    j2.Use.acceptA(j2.Use.returnA())
    j2.Use.returnA().id2()
}