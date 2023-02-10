// ISSUE: KT-24779

class Inv<T>(val data: T)

fun test1(x: Inv<out String?>) {
    val y = x.data
    when (y) {
        is String -> x.data.length // Smart cast: x.data is String
    }
}

fun test2(x: Inv<out Any?>) {
    val y = x.data
    when (y) {
        is String -> x.data.length // No smart cast, UNRESOLVED_REFERENCE: length
    }
}
