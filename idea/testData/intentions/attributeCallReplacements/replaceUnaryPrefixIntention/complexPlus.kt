fun test() {
    class Test {
        fun plus(): Test = Test()
        fun plus(a: Test): Test = Test()
        fun minus(): Test = Test()
    }
    val test = Test()
    println(-(test + test).pl<caret>us().toString())
}
