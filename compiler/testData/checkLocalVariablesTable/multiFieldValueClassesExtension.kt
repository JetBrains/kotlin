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
// VARIABLE : NAME=$this$foo-a-data TYPE=I INDEX=*
// VARIABLE : NAME=$this$foo-boolean TYPE=Z INDEX=*
// VARIABLE : NAME=<anonymous-context-parameter-ValueClassTest>-a-data TYPE=I INDEX=*
// VARIABLE : NAME=<anonymous-context-parameter-ValueClassTest>-boolean TYPE=Z INDEX=*
// VARIABLE : NAME=<anonymous-context-parameter-Int> TYPE=I INDEX=*
// VARIABLE : NAME=<anonymous-context-parameter-UInt>-data TYPE=I INDEX=*
// VARIABLE : NAME=arg0 TYPE=I INDEX=*
// VARIABLE : NAME=c1 TYPE=I INDEX=*
// VARIABLE : NAME=c2-data TYPE=I INDEX=*
// VARIABLE : NAME=c3-a-data TYPE=I INDEX=*
// VARIABLE : NAME=c3-boolean TYPE=Z INDEX=*
// VARIABLE : NAME=x TYPE=I INDEX=*
// VARIABLE : NAME=y-data TYPE=I INDEX=*
// VARIABLE : NAME=z-a-data TYPE=I INDEX=*
// VARIABLE : NAME=z-boolean TYPE=Z INDEX=*
