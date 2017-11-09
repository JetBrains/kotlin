fun foo() {
    var b: Boolean? = null
    val x = !(b <caret>?: true)
}