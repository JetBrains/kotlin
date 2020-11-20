class Foo {
    @field:java.lang.Deprecated
    val bar: String = "123"

    @java.lang.Deprecated
    val bar2: String = "123"

    @java.lang.Deprecated
    fun test() {}

}
