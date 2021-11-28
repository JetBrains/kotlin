// KOTLIN_CONFIGURATION_FLAGS: SAM_CONVERSIONS=CLASS
// WITH_STDLIB

class A {
    fun test1() {
        val f = { }
        val t1 = Runnable(f)
        val t2 = Runnable(f)
    }
}

class B {
    fun test2() {
        val f = { }
        val t1 = Runnable(f)
        val t2 = Runnable(f)
    }
}
