// WITH_STDLIB
// WITH_REFLECT
// FULL_JDK
import java.lang.reflect.Field

@Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
fun <@kotlin.internal.OnlyInputTypes T> assertEquals(expected: T, actual: T): T = actual

fun test(field: Field) {
    <!DEBUG_INFO_EXPRESSION_TYPE("java.lang.Class<out kotlin.Any>?")!>assertEquals(
        <!DEBUG_INFO_EXPRESSION_TYPE("java.lang.Class<*>..java.lang.Class<*>?!")!>field.type<!>,
        <!DEBUG_INFO_EXPRESSION_TYPE("java.lang.Class<kotlin.Long>?")!>Long::class.javaPrimitiveType<!>
    )<!>
}
