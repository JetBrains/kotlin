enum class Z {
    ENTRY {
        fun test() {}

        class A {
            fun test2() {
                test()
            }
        }
    }
}