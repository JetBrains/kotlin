package test

fun test() {
    var captured: Any = "str"

    class Local {
        fun assign() {
            captured = 1
        }

        fun unrelated(): Int = 2
    }

    if (captured is String) {
        <expr>captured</expr>
    }
}
