// WITH_STDLIB
// IGNORE_K1

@JvmInline
value class InlineClassTest(val a: UInt) {
    context(_: Int, _: UInt, c1: Int, c2: UInt)
    fun UInt.foo(x: Int, y: UInt) {
        val arg0 = 42
    }
}

// METHOD : InlineClassTest.foo-2L4_mC8(IIIIIIII)V
// VARIABLE : NAME=$context-Int TYPE=I
// VARIABLE : NAME=$context-UInt TYPE=I
// VARIABLE : NAME=$this$foo TYPE=I
// VARIABLE : NAME=arg0 TYPE=I
// VARIABLE : NAME=arg0 TYPE=I
// VARIABLE : NAME=c1 TYPE=I
// VARIABLE : NAME=c2 TYPE=I
// VARIABLE : NAME=x TYPE=I
// VARIABLE : NAME=y TYPE=I
