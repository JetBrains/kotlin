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
// VARIABLE : NAME=$context-Int TYPE=I INDEX=*
// VARIABLE : NAME=$v$c$InlineClassTest$-this$0 TYPE=I INDEX=*
// VARIABLE : NAME=$v$c$kotlin-UInt$-$context-UInt$0 TYPE=I INDEX=*
// VARIABLE : NAME=$v$c$kotlin-UInt$-$this$foo$0 TYPE=I INDEX=*
// VARIABLE : NAME=$v$c$kotlin-UInt$-c2$0 TYPE=I INDEX=*
// VARIABLE : NAME=$v$c$kotlin-UInt$-y$0 TYPE=I INDEX=*
// VARIABLE : NAME=arg0 TYPE=I INDEX=*
// VARIABLE : NAME=c1 TYPE=I INDEX=*
// VARIABLE : NAME=x TYPE=I INDEX=*
