class A {
    inner class C(val m: String) {
        fun test(): String {
            m.f(::C)
            return m
        }
    }
}

inline fun String.f(g: (String) -> A.C): A.C = g(this)

fun box(): String = A().C("OK").test()
