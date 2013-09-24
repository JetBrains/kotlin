fun box() : String {
    fun foo<T>(t:() -> T) : T = t()

    return foo {}
    return "OK"
}