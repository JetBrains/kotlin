// IGNORE_BACKEND_K1: ANY
// REASON: KT-25341

inline fun foo(): String {
    return object {
        fun func(): String {
            abstract class A
            fun local() {}
            open class B
            class C
            data class D(val i: Int)

            local()
            B()
            C()
            D(42)
            return "O"
        }
    }.func()
}

inline val bar: String get() {
    return object {
        fun func(): String {
            abstract class A
            fun local() {}
            open class B
            class C
            data class D(val i: Int)

            local()
            B()
            C()
            D(42)
            return "K"
        }
    }.func()
}

fun box(): String = foo() + bar
