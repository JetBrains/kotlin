import test.*

public class C {
    fun test() {
        D.m(B.n())
    }
}

public class D {
    companion object {
        @JvmStatic
        @Suppress("UNUSED_PARAMETER")
        fun m(o: Any?) {}
    }
}