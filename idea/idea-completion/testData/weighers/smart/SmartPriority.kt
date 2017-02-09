var nonNullable: C = C()

class C {
    var nullableX: C? = null
    var nullableFoo: C? = null

    fun foo(pFoo: C, s: String) {
        val local = C()
        foo(<caret>)
    }
}

// ORDER: pFoo
// ORDER: nullableFoo
// ORDER: nullableFoo
// ORDER: "pFoo, s"
// ORDER: this
// ORDER: local
// ORDER: nonNullable
// ORDER: also
// ORDER: apply
// ORDER: nullableX
// ORDER: nullableX
// ORDER: takeIf
// ORDER: takeIf
// ORDER: takeUnless
// ORDER: takeUnless
// ORDER: C
