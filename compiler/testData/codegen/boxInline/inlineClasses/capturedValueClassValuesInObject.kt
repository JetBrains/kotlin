// WITH_STDLIB
// NO_CHECK_LAMBDA_INLINING

// FILE: inline.kt

inline class IC(val value: String)

interface Producer {
    fun produce(): String
}

inline fun capture(u: UInt, ic: IC, crossinline suffix: () -> String): Producer {
    val localU = u + 1u
    val localIc = IC(ic.value + "!")
    return object : Producer {
        override fun produce(): String = "$u ${ic.value} $localU ${localIc.value} ${suffix()}"
    }
}

// FILE: box.kt

fun box(): String {
    val k = "K"
    val result = capture(1u, IC("O"), { k }).produce()
    return if (result == "1 O 2 O! K") "OK" else "Fail: $result"
}
