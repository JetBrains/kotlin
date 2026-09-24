// WITH_STDLIB

@JvmInline
value class InlineClassTest(val a: UInt) {
    context(_: Int, _: UInt, c1: Int, c2: UInt)
    fun UInt.foo(x: Int, y: UInt) {
        val arg0 = 42
    }
}

// METHOD : InlineClassTest.foo-2L4_mC8(IIIIIIII)V
// VARIABLE : NAME=$context-Int TYPE=I
// VARIABLE : NAME=$v$c$InlineClassTest$-this TYPE=I
// VARIABLE : NAME=$v$c$kotlin-UInt$-$context-UInt TYPE=I
// VARIABLE : NAME=$v$c$kotlin-UInt$-$this$foo TYPE=I
// VARIABLE : NAME=$v$c$kotlin-UInt$-c2 TYPE=I
// VARIABLE : NAME=$v$c$kotlin-UInt$-y TYPE=I
// VARIABLE : NAME=arg0 TYPE=I
// VARIABLE : NAME=c1 TYPE=I
// VARIABLE : NAME=x TYPE=I
