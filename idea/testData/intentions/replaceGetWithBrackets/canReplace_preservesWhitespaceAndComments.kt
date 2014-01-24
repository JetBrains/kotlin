class C {
    fun get(param1: String, param2: Int, param3: Int) = param1 + param2 + param3
    fun test() = C().g<caret>et("""foo
        bar baz""",4, // biff
    2)
}
