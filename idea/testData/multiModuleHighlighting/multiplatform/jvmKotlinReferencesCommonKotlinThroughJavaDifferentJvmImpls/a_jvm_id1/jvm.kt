package common

actual class A {
    fun id1() {}
}


fun use() {
    j1.Use.acceptA(j1.Use.returnA())
    j1.Use.returnA().id1()
}
