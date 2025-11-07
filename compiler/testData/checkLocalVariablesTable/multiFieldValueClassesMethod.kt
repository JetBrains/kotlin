// WITH_STDLIB
// LANGUAGE: +ValueClasses
// IGNORE_K1

@JvmInline
value class ValueClassTest(val a: UInt, val boolean: Boolean) {
    context(_: Int, _: UInt, _: ValueClassTest, c1: Int, c2: UInt, c3: ValueClassTest)
    fun UInt.foo(x: Int, y: UInt, z: ValueClassTest) {
        val arg0 = 42
    }
}

// METHOD : ValueClassTest.foo-wR1AMpA(IZIIIZIIIZIIIIZ)V
// VARIABLE : NAME=$context-Int TYPE=I INDEX=*
// VARIABLE : NAME=$context-UInt TYPE=I INDEX=*
// VARIABLE : NAME=$context-ValueClassTest-a TYPE=I INDEX=*
// VARIABLE : NAME=$context-ValueClassTest-boolean TYPE=Z INDEX=*
// VARIABLE : NAME=$dispatchReceiver-a TYPE=I INDEX=*
// VARIABLE : NAME=$dispatchReceiver-boolean TYPE=Z INDEX=*
// VARIABLE : NAME=<this> TYPE=I INDEX=*
// VARIABLE : NAME=arg0 TYPE=I INDEX=*
// VARIABLE : NAME=c1 TYPE=I INDEX=*
// VARIABLE : NAME=c2 TYPE=I INDEX=*
// VARIABLE : NAME=c3-a TYPE=I INDEX=*
// VARIABLE : NAME=c3-boolean TYPE=Z INDEX=*
// VARIABLE : NAME=x TYPE=I INDEX=*
// VARIABLE : NAME=y TYPE=I INDEX=*
// VARIABLE : NAME=z-a TYPE=I INDEX=*
// VARIABLE : NAME=z-boolean TYPE=Z INDEX=*