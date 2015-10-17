// DISABLE-ERRORS
// IS_APPLICABLE: false
import JavaMethods

fun foo(javaClass: JavaMethods) {
    val v1 = javaClass.get(1)
    val v2 = javaClass.get("a", 1)
}