// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 forEach
fun box(): String {
    var result = ""
    val predicate: (Int) -> Int = { x -> x * 2 }
    val predicate2: (Int) -> Boolean = { x -> x != 4 }
    val predicate3: (Int) -> Unit = { x -> if (x == 6) result += "K" else if (x == 2) result += "O" else result += x.toString() }
    val seq = sequenceOf(1, 2, 3).map(predicate).filter(predicate2).forEach(predicate3)
    return result
}
