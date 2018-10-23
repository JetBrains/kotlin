// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

inline class A(val a: Any)

inline class NA(val b: Any?)

fun box(): String {
    val ns1 = NA(A("abc"))
    val ns2 = NA(null)
    val t = "-$ns1-$ns2-"
    if (t != "-NA(b=A(a=abc))-NA(b=null)-") return throw AssertionError(t)
    return "OK"
}