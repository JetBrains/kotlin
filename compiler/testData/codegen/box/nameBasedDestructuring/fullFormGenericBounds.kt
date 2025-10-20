// LANGUAGE: +NameBasedDestructuring
// WITH_STDLIB

class GenericNonNullBox<T : Any>(val nbValue: T)

class GenericNumberBox<T : Number>(val nbNum: T)

class GenericNullableBox<T>(val nbOpt: T?)

fun pickString(x: String) = "OK"
fun pickString(x: Any) = "FAIL-Any"

fun pickInt(x: Int) = "OK"
fun pickInt(x: Number) = "FAIL-Number"

fun pickNullableString(x: String?) = "OK"
fun pickNullableString(x: Any?) = "FAIL-Any?"

fun box(): String {
    val nonNullBox = GenericNonNullBox("")
    (val s = nbValue) = nonNullBox
    if (pickString(s) != "OK") return "FAIL: String inference"

    val numberBox = GenericNumberBox(1)
    (val i = nbNum) = numberBox
    if (pickInt(i) != "OK") return "FAIL: Int inference"

    val nullableBoxNull = GenericNullableBox<String>(null)
    (val optNull = nbOpt) = nullableBoxNull
    if (pickNullableString(optNull) != "OK") return "FAIL: String? inference (null case)"

    val nullableBoxValue = GenericNullableBox("")
    (val optVal = nbOpt) = nullableBoxValue
    if (pickNullableString(optVal) != "OK") return "FAIL: String? inference (value case)"

    return "OK"
}
