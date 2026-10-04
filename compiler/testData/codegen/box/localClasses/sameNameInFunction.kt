// ISSUE: KT-84623

fun localClasses(flag: Boolean, value: String): String {
    return if (flag) {
        class Local {
            inner class Inner {
                fun get() = "O" + value
            }

            val anonymous = object {
                fun get() = Inner().get()
            }
        }
        Local().anonymous.get()
    } else {
        class Local {
            inner class Inner {
                fun get() = value + "K"
            }

            val anonymous = object {
                fun get() = Inner().get()
            }
        }
        Local().anonymous.get()
    }
}

fun box(): String {
    val first = localClasses(true, "K")
    if (first != "OK") return "first: $first"
    val second = localClasses(false, "O")
    if (second != "OK") return "second: $second"
    return "OK"
}
