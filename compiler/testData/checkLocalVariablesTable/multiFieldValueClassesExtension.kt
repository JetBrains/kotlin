// WITH_STDLIB
// LANGUAGE: +JvmInlineMultiFieldValueClasses
// IGNORE_K1

@JvmInline
value class ValueClassTest(val a: UInt, val boolean: Boolean)

context(_: Int, _: UInt, _: ValueClassTest, c1: Int, c2: UInt, c3: ValueClassTest)
fun ValueClassTest.foo(x: Int, y: UInt, z: ValueClassTest) {
    val arg0 = 42
}

// METHOD : MultiFieldValueClassesExtensionKt.foo-09T95H0(IIIZIIIZIZIIIZ)V
// VARIABLE : NAME=$context-Int TYPE=I
// VARIABLE : NAME=$context-UInt TYPE=I
// VARIABLE : NAME=$context-ValueClassTest-a TYPE=I
// VARIABLE : NAME=$context-ValueClassTest-boolean TYPE=Z
// VARIABLE : NAME=$this$foo-a TYPE=I
// VARIABLE : NAME=$this$foo-boolean TYPE=Z
// VARIABLE : NAME=arg0 TYPE=I
// VARIABLE : NAME=c1 TYPE=I
// VARIABLE : NAME=c2 TYPE=I
// VARIABLE : NAME=c3-a TYPE=I
// VARIABLE : NAME=c3-boolean TYPE=Z
// VARIABLE : NAME=x TYPE=I
// VARIABLE : NAME=y TYPE=I
// VARIABLE : NAME=z-a TYPE=I
// VARIABLE : NAME=z-boolean TYPE=Z
