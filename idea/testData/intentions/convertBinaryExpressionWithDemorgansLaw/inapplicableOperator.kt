// IS_APPLICABLE: false
// ERROR: Unresolved reference. None of the following candidates is applicable because of receiver type mismatch: <br>public operator fun String?.plus(other: Any?): String defined in kotlin
fun foo(a: Boolean, b: Boolean) : Boolean {
    return !<caret>(!a + b)
}
