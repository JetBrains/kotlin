// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY

data class MyContainer(var i: Int)

var operationScore = 0

context(_context0: Int)
operator fun MyContainer.get(index: Int): Int {
    operationScore += _context0
    return if (index == 0) i else -1
}

context(_context0: Int)
operator fun MyContainer.plusAssign(other: MyContainer) {
    operationScore += _context0
    i += other.i
}

context(_context0: Int)
operator fun MyContainer.inc(): MyContainer {
    operationScore += _context0
    return MyContainer(i + 1)
}

fun box(): String {
    var myContainer = MyContainer(0)
    with(1) {
        myContainer += MyContainer(myContainer++[0])
    }
    return if (myContainer.i == 1 && operationScore == 3) "OK" else "fail"
}
