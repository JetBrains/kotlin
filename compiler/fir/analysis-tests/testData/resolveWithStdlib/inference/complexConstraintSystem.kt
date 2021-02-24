class Inv<X>(val x: X)

fun test_0(list: List<Int>, b: Boolean) {
    val x = list.mapNotNull { if (b) Inv(it) else null }.first().x
}