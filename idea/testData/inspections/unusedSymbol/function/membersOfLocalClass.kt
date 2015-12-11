fun main(args: Array<String>) {
    class LocalClass {
        fun f() {
        }

        @Suppress("UnusedSymbol")
        fun fNoWarn() {}

        val p = 5
    }

    @Suppress("UnusedSymbol")
    class OtherClass {
        fun fNoWarn() {}
    }


    LocalClass().f()
    LocalClass().p
}

@Suppress("UnusedSymbol")
fun other() {
    class OtherClass {
        fun fNoWarn() {}
    }
}