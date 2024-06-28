// FIR_IDENTICAL
fun <T: Comparable<T>> arrayData(vararg values: T, toArray: Array<T>.() -> Unit) {}

fun <T : Long> arrayLongInheritedData(vararg values: T, toArray: Array<T>.() -> Unit) {}

fun longArrayData(vararg values: Long, toArray: LongArray.() -> Unit) {}

fun shortArrayData(vararg values: Short, toArray: ShortArray.() -> Unit) {}

fun arrayOfLongData(vararg values: Long, toArray: Array<Long>.() -> Unit) {}

fun arrayOfShortData(vararg values: Short, toArray: Array<Short>.() -> Unit) {}

fun box(): String {
    arrayData(42) { }
    arrayLongInheritedData(42) { }
    longArrayData(42) { }
    shortArrayData(42) { }
    arrayOfLongData(42) { }
    arrayOfShortData(42) { }
    return "OK"
}
