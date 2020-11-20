class Test {
    var serial: String = ""
        set(value) {
            field = value.toUpperCase()
        }
    var name: String = ""
    val age by lazy { 15 + 10 }
    val color: String
        get() = "Purple"
    <caret>
}