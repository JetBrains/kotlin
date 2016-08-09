// From KT-13324: always succeeds
val x = null as String?
// From KT-260: sometimes succeeds
fun foo(a: String?): Int? {
    val c = a as? Int?
    return c
}