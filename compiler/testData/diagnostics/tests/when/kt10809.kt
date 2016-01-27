// !DIAGNOSTICS: -UNUSED_PARAMETER -DEBUG_INFO_SMARTCAST

interface Data
interface Item
class FlagData(val value: Boolean) : Data
class ListData<T : Item>(val list: List<T>) : Data

fun <T> listOf(vararg items: T): List<T> = null!!

fun test1(o: Any) = <!TYPE_INFERENCE_FAILED_ON_SPECIAL_CONSTRUCT!>when<!> (o) {
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
        <!TYPE_INFERENCE_FAILED_ON_SPECIAL_CONSTRUCT!>if<!> (true)
            ListData(listOf())
        else
            FlagData(true)

fun test2x(): Data =
        if (true) ListData(listOf()) else FlagData(true)

fun test2y(): Any =
        if (true) ListData(listOf()) else FlagData(true)

fun test2z(): Any =
        run { if (true) ListData(listOf()) else FlagData(true) }

