// From KT-13324: always succeeds
val x = null as String?
// From KT-260: sometimes succeeds
fun foo(a: String?): Int? {
    val c = a <!CAST_NEVER_SUCCEEDS!>as?<!> Int?
    return c
}