class C {
    <caret>@deprecated("")
    val foo: String
        get() = bar()

    fun bar(): String = ""
}
