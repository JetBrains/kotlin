var nonNullable: Boolean = true

var nullableX: Boolean? = null
var nullableFoo: Boolean? = null

fun foo(pFoo: Boolean, s: String) {
    val local = true
    foo(<caret>)
}

// ORDER: "pFoo, s"
// ORDER: pFoo
// ORDER: nullableFoo
// ORDER: nullableFoo
// ORDER: true
// ORDER: false
// ORDER: local
// ORDER: ASSERTIONS_ENABLED
// ORDER: nonNullable
// ORDER: nullableX
// ORDER: nullableX
