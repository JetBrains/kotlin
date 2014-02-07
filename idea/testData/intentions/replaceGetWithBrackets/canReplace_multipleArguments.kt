class C {
    fun get(param1: String, param2: Int) = param1 + param2
    fun test() = C().g<caret>et("foo", 42)
}
