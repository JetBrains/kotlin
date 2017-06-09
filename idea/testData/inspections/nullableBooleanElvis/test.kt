fun foo() {
    var a: Boolean? = null
    var b: Boolean? = null
    if (a ?: false) {

    }
    if (!(a ?: false)) {

    }
    if (a ?: false || !(b ?: true)) {

    }
    val x = a ?: false
    val y = !(b ?: true)
}