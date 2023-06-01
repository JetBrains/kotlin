// !LANGUAGE: +ContextReceivers

data class MyContainer(var i: Int)

var operationScore = 0

context(Int)
operator fun MyContainer.get(index: Int): Int {
    operationScore += this@Int
    return if (index == 0) i else -1
}

context(Int)
operator fun MyContainer.plusAssign(other: MyContainer) {
    operationScore += this@Int
    i += other.i
}

context(Int)
operator fun MyContainer.inc(): MyContainer {
    operationScore += this@Int
    return MyContainer(i + 1)
}

fun box(): String {
    var myContainer = MyContainer(0)
    with(1) {
        myContainer += MyContainer(myContainer++[0])
    }
    return if (myContainer.i == 1 && operationScore == 3) "OK" else "fail"
}
