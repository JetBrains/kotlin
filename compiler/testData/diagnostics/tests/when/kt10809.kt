// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER -DEBUG_INFO_SMARTCAST
// NI_EXPECTED_FILE
/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-155, test type: pos):
 *  - expressions, when-expression -> paragraph 5 -> sentence 1
 *  - expressions, when-expression -> paragraph 6 -> sentence 1
 *  - expressions, when-expression -> paragraph 2 -> sentence 1
 *  - expressions, when-expression -> paragraph 9 -> sentence 1
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 1
 *  - expressions, conditional-expression -> paragraph 4 -> sentence 1
 *  - declarations, function-declaration -> paragraph 7 -> sentence 1
 *  - type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 1
 *  - type-system, type-kinds, type-parameters -> paragraph 4 -> sentence 1
 *  - type-inference, local-type-inference -> paragraph 8 -> sentence 1
 *  - type-inference, local-type-inference -> paragraph 2 -> sentence 1
 */

interface Data
interface Item
class FlagData(val value: Boolean) : Data
class ListData<T : Item>(val list: List<T>) : Data

fun <T> listOf(vararg items: T): List<T> = null!!

fun test1(o: Any) = <!OI;TYPE_INFERENCE_FAILED_ON_SPECIAL_CONSTRUCT!>when<!> (o) {
    is List<*> ->
        ListData(listOf())
    is Int -> when {
        o < 0 ->
            FlagData(true)
        else ->
            null
    }
    else ->
        null
}

fun test1x(o: Any): Data? = when (o) {
    is List<*> ->
        ListData(listOf())
    is Int -> when {
        o < 0 ->
            FlagData(true)
        else ->
            null
    }
    else ->
        null
}

fun test2() =
        <!OI;TYPE_INFERENCE_FAILED_ON_SPECIAL_CONSTRUCT!>if<!> (true)
            ListData(listOf())
        else
            FlagData(true)

fun test2x(): Data =
        if (true) ListData(listOf()) else FlagData(true)

fun test2y(): Any =
        if (true) ListData(listOf()) else FlagData(true)

fun test2z(): Any =
        run { if (true) ListData(listOf()) else FlagData(true) }

