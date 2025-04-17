// WITH_STDLIB
// IGNORE_K1

class ClassTest(val a: Boolean) {
    context(_: Int, _: Int, _: Long, c1: Int, c2: Int, c3: Long)
    fun foo(x: Int) {
        val arg0 = 42
    }
}

// METHOD : ClassTest.foo(IIJIIJI)V
// VARIABLE : NAME=$context-Int#1 TYPE=I INDEX=*
// VARIABLE : NAME=$context-Int#2 TYPE=I INDEX=*
// VARIABLE : NAME=$context-Long TYPE=J INDEX=*
// VARIABLE : NAME=arg0 TYPE=I INDEX=*
// VARIABLE : NAME=c1 TYPE=I INDEX=*
// VARIABLE : NAME=c2 TYPE=I INDEX=*
// VARIABLE : NAME=c3 TYPE=J INDEX=*
// VARIABLE : NAME=this TYPE=LClassTest; INDEX=*
// VARIABLE : NAME=x TYPE=I INDEX=*
