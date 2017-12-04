package codegen.enum.varargParam

import kotlin.test.*

enum class Piece(vararg val states: Int) {
    I(3, 4, 5)
}

@Test fun runTest() {
    println(Piece.I.states[0])
}