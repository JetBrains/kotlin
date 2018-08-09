fun box() : String {
    fun <T> foo(t:() -> T) : T = t()

    return foo {"OK"}
}