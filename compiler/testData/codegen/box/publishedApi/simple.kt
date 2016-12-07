class A {

    @PublishedApi
    internal fun published() = "OK"

    inline fun test() = published()

}

fun box() : String {
    return A().test()
}