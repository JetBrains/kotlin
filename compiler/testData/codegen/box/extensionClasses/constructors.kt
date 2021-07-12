// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

class A(val ok: String)

context(A)
class B(oValue: Boolean = true, kValue: Boolean = true) {
    var o: Boolean
    var k: Boolean

    init {
        o = oValue
        k = kValue
    }

    constructor(oValue: String, kValue: String) : this(oValue == "O", kValue == "K")

    fun result() = if (o && k) ok else "fail"
}

fun box(): String {
    val a = A("OK")
    with (a) {
        val results = listOf(
            B(true, true).result(),
            B("O", "K").result(),
            B().result()
        )
        return if (results.all { it == "OK" }) "OK" else "fail"
    }
}