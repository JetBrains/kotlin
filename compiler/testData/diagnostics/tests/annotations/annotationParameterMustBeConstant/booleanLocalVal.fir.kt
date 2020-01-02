annotation class Ann(vararg val i: Boolean)
fun foo() {
    val bool1 = true

    @Ann(bool1) val a = bool1
}