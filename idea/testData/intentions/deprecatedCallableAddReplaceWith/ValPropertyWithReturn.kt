class C {
    <caret>@deprecated("")
    val foo: String
        get() {
            return bar()
        }

    fun bar(): String = ""
}
