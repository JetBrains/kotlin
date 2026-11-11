// ISSUE: KT-84623

class LocalClassInFieldInitializer(flag: Boolean, value: String) {
    val result = if (flag) {
        class Local {
            fun get() = "O" + value
        }
        Local().get()
    } else {
        class Local {
            fun get() = value + "K"
        }
        Local().get()
    }
}

fun box(): String {
    val first = LocalClassInFieldInitializer(true, "K").result
    if (first != "OK") return "first: $first"
    val second = LocalClassInFieldInitializer(false, "O").result
    if (second != "OK") return "second: $second"
    return "OK"
}
