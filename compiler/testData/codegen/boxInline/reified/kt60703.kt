// FILE: 1.kt

inline fun <reified T> foo1(x: T): T {
    return object {
        fun bar1(y: T) = y
    }.bar1(x)
}

inline fun <reified T> foo2(z: T): T {
    return with(object {
        fun T.bar2(): T {
            return this
        }
    }) { z.bar2() }
}

inline fun <reified T> foo3(z: T): T {
    return object {
        fun bar3(): T {
            val p = z
            return p
        }
    }.bar3()
}

inline fun <reified T> foo4(z: T): T {
    return object {
        val f = z
        fun bar4(): T {
            return f
        }
    }.bar4()
}

// FILE: 2.kt

fun box() = ('N' + foo1(0) + foo2(1)).toString() + ('F' + foo3(2) + foo4(3))