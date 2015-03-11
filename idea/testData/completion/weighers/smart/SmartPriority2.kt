var nonNullable: C = C()
var nullableX: C? = null
var nullableFoo: C? = null

abstract class C {
    default object {
        val INSTANCE_X = C()
        val INSTANCE_FOO = C()
    }
}

fun foo(pFoo: C, s: String) {
    val local = C()
    foo(<caret>)
}


// ORDER: "pFoo, s"
// ORDER: pFoo
// ORDER: nullableFoo
// ORDER: nullableFoo
// ORDER: INSTANCE_FOO
// ORDER: local
// ORDER: nonNullable
// ORDER: nullableX
// ORDER: nullableX
// ORDER: INSTANCE_X
// ORDER: object
