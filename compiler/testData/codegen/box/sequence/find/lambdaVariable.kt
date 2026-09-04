// WITH_STDLIB

fun box(): String {
    val predicate: (Int) -> Boolean = { x -> x == 2 }
    val result = sequenceOf(1, 2, 3).find(predicate)
    if (result != 2) return "failed: expected 2, but got $result"
    val predicate2: (Int) -> Boolean = { x -> x != 2 }
    val result2 = sequenceOf(2, 3, 4).first(predicate2)
    if (result2 != 3) return "failed: expected 3, but got $result2"
    return "OK"
}
