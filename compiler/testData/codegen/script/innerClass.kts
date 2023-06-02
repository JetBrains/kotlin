// IGNORE_BACKEND_K2: JVM_IR

val b = "K"

inner class InnerClass(val s: String) {
    fun test1() = s + b

    inner class C1 {
        fun bar(c: String) = s + b + c
    }

    fun test2() = C1().bar("!")

    inner class C2 {
        fun bar(c: String) = s + b + c

        fun test(): String {
            var c = "?"
            return object {
                fun run() = s + b + c
            }.run()
        }
    }

    fun test3() = C2().test()
}

val rv = InnerClass("O").test1() + InnerClass("_O").test2() + InnerClass("__O").test3()

// expected: rv: OK_OK!__OK?
