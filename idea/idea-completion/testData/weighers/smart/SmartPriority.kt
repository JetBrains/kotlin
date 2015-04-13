var nonNullable: C = C()

class C {
    var nullableX: C? = null
    var nullableFoo: C? = null

    fun foo(pFoo: C, s: String) {
        val local = C()
        foo(<caret>)
    }
}

// ORDER: "pFoo, s"
// ORDER: pFoo
// ORDER: nullableFoo
// ORDER: nullableFoo
// ORDER: this
// ORDER: local
// ORDER: nonNullable
// ORDER: nullableX
// ORDER: nullableX
// ORDER: C
