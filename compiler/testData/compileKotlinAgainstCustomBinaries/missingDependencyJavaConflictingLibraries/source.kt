import test.*

fun testA(b1: B1, b2: B2) {
    b2.consumeA(b1.produceA())

    // No error here as in javac
    b2.consumeListOfAs(b1.produceListOfAs())
}
