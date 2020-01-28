// IGNORE_BACKEND: JVM_IR
// LOCAL_VARIABLE_TABLE

fun foo() {
    fun a() {}
    fun a2() {}
    fun a2(a: Int) {}
    fun `b c`() {}
    fun `c$d`() {}
}