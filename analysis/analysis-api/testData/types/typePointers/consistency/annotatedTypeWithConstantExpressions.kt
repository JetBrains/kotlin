// WITH_STDLIB
// FILE: constants.kt
package constants

const val MY_INT = 3
const val MY_STRING = "str"

enum class E { A, B }

// FILE: main.kt
import constants.MY_INT
import constants.MY_STRING
import constants.E
import constants.E.A
import kotlin.reflect.KClass

@Target(AnnotationTarget.TYPE)
annotation class Nested(val i: Int = 0, vararg val classes: KClass<*>)

@Target(AnnotationTarget.TYPE)
annotation class Complex(
    val template: String,
    val concatenation: String,
    val negative: Int,
    val shift: Long,
    val maxValue: Int,
    val length: Int,
    val logic: Boolean,
    val unsigned: UInt,
    val unsignedArray: UIntArray,
    val importedEntry: E,
    val qualifiedEntry: E,
    val targets: Array<AnnotationTarget>,
    val nestedArray: Array<Nested>,
    vararg val x: Int,
)

fun test(
    value: <expr>@Complex(
        template = "a${MY_STRING}b$MY_INT",
        concatenation = MY_STRING + "-" + MY_INT,
        negative = -MY_INT,
        shift = 1L shl MY_INT,
        maxValue = Int.MAX_VALUE,
        length = MY_STRING.length,
        logic = MY_INT > 2 && !false,
        unsigned = MY_INT.toUInt() + 4u,
        unsignedArray = uintArrayOf(1u, 2u),
        importedEntry = A,
        qualifiedEntry = constants.E.B,
        targets = [AnnotationTarget.TYPE, kotlin.annotation.AnnotationTarget.CLASS],
        nestedArray = [Nested(classes = [String::class]), Nested(i = MY_INT, classes = *arrayOf(Int::class, E::class))],
        x = *intArrayOf(MY_INT, MY_INT + 1),
    ) String</expr>
) {
}
