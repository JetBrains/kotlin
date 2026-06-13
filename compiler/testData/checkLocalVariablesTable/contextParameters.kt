// WITH_STDLIB

class ClassTest(val a: Boolean) {
    context(_: Int, _: Int, _: Long, c1: Int, c2: Int, c3: Long)
    fun foo(x: Int) {
        val arg0 = 42
    }
}

// METHOD : ClassTest.foo(IIJIIJI)V
// VARIABLE : NAME=$context-Int$1 TYPE=I
// VARIABLE : NAME=$context-Int$2 TYPE=I
// VARIABLE : NAME=$context-Long TYPE=J
// VARIABLE : NAME=arg0 TYPE=I
// VARIABLE : NAME=c1 TYPE=I
// VARIABLE : NAME=c2 TYPE=I
// VARIABLE : NAME=c3 TYPE=J
// VARIABLE : NAME=this TYPE=LClassTest;
// VARIABLE : NAME=x TYPE=I
