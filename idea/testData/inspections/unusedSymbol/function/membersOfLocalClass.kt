fun main(args: Array<String>) {
    class LocalClass {
        fun f() {
        }

        @Suppress("unused")
        fun fNoWarn() {}

        val p = 5
    }

    @Suppress("unused")
    class OtherClass {
        fun fNoWarn() {}
    }


    LocalClass().f()
    LocalClass().p
}

@Suppress("unused")
fun other() {
    class OtherClass {
        fun fNoWarn() {}
    }
}