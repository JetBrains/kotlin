// WITH_STDLIB
// LANGUAGE: +ValueClasses
// IGNORE_K1

@JvmInline
value class ValueClassTest(val a: UInt, val boolean: Boolean)

context(_: Int, _: UInt, _: ValueClassTest, c1: Int, c2: UInt, c3: ValueClassTest)
fun ValueClassTest.foo(x: Int, y: UInt, z: ValueClassTest) {
    val arg0 = 42
}

// METHOD : MultiFieldValueClassesExtensionKt.foo-09T95H0(IIIZIIIZIZIIIZ)V
// VARIABLE : NAME=$context-Int TYPE=I INDEX=*
// VARIABLE : NAME=$v$c$ValueClassTest$-$context-ValueClassTest$0 TYPE=I INDEX=*
// VARIABLE : NAME=$v$c$ValueClassTest$-$context-ValueClassTest$1 TYPE=Z INDEX=*
// VARIABLE : NAME=$v$c$ValueClassTest$-$this$foo$0 TYPE=I INDEX=*
// VARIABLE : NAME=$v$c$ValueClassTest$-$this$foo$1 TYPE=Z INDEX=*
// VARIABLE : NAME=$v$c$ValueClassTest$-c3$0 TYPE=I INDEX=*
// VARIABLE : NAME=$v$c$ValueClassTest$-c3$1 TYPE=Z INDEX=*
// VARIABLE : NAME=$v$c$ValueClassTest$-z$0 TYPE=I INDEX=*
// VARIABLE : NAME=$v$c$ValueClassTest$-z$1 TYPE=Z INDEX=*
// VARIABLE : NAME=$v$c$kotlin-UInt$-$context-UInt$0 TYPE=I INDEX=*
// VARIABLE : NAME=$v$c$kotlin-UInt$-c2$0 TYPE=I INDEX=*
// VARIABLE : NAME=$v$c$kotlin-UInt$-y$0 TYPE=I INDEX=*
// VARIABLE : NAME=arg0 TYPE=I INDEX=*
// VARIABLE : NAME=c1 TYPE=I INDEX=*
// VARIABLE : NAME=x TYPE=I INDEX=*
