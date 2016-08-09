// From KT-13324: always succeeds
val x = null <!USELESS_CAST!>as String?<!>
// From KT-260: sometimes succeeds
fun foo(a: String?): Int? {
    val c = a as? Int?
    return c
}