package c

import b.B1
import b.B2

fun testA(b1: B1, b2: B2) {
    b2.consumeA(b1.produceA())
    b2.consumeA(b1.produceAGeneric("foo"))
}

fun testAA(b1: B1, b2: B2) {
    b2.consumeAA(b1.produceAA())
}

fun testAAA(b1: B1, b2: B2) {
    b2.consumeAAA(b1.produceAAA())
}