// TARGET_BACKEND: JVM
// WITH_STDLIB

open class A {

    companion object {

        @JvmStatic
        protected const val x = 1

        @JvmStatic
        @JvmField
        protected val z = 1
    }
}

class B : A() {

    // We have to do it like this because the protected members are not accessible outside of subclasses.
    fun box(): String {
        if (x != 1) return "fail"
        if (z != 1) return "fail2"
        return "OK"
    }
}

fun box(): String = B().box()
