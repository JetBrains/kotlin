// WITH_RUNTIME

@JvmInline
value class A(val a: Any)

@JvmInline
value class NA(val b: Any?)

fun box(): String {
    val ns1 = NA(A("abc"))
    val ns2 = NA(null)
    val t = "-$ns1-$ns2-"
    if (t != "-NA(b=A(a=abc))-NA(b=null)-") return throw AssertionError(t)
    return "OK"
}