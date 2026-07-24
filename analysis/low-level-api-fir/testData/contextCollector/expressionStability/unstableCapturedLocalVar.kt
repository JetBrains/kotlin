fun test() {
    var value: String? = ""

    fun update() {
        value = null
    }

    if (value != null) {
        update()
        <expr>value</expr>
    }
}
