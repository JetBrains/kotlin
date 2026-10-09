// ISSUE: KT-84623
// WITH_STDLIB

class A {
    var result = ""

    init {
        class Local {
            fun get() = "O"
        }
        result += if (Local::class.simpleName == "Local") Local().get() else "first name"
    }

    init {
        class Local {
            fun get() = "K"
        }
        result += if (Local::class.simpleName == "Local") Local().get() else "second name"
    }

    init {
        class Local {
            fun get() = ""
        }
        result += if (Local::class.simpleName == "Local") Local().get() else "third name"
    }

    class `1Local`
}

fun box(): String = A().result
