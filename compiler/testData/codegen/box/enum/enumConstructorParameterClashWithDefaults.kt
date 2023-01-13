// KT-55828
// IGNORE_BACKEND_K2: NATIVE
enum class A(
    name: String,
    ordinal: Int
) {
    FOO("foo", 4),
    BAR("bar", 5);

    val testName = name
    val testOrdinal = ordinal
}

fun box(): String {
    val fooName =  A.FOO.testName == "foo"
    val fooOrdinal = A.FOO.testOrdinal == 4

    val barName = A.BAR.testName == "bar"
    val barOrdinal = A.BAR.testOrdinal == 5

    return if (fooName && fooOrdinal && barName && barOrdinal) "OK" else "fail"
}
