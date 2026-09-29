package test

fun test() {
    var captured: Any = "str"

    class Local {
        fun assign() {
            captured = 1
        }

        fun read() {
            if (captured is String) {
                <expr>captured</expr>
            }
        }
    }
}
