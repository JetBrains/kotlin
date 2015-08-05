var nonNullable: Boolean = true

var nullableX: Boolean? = null
var nullableFoo: Boolean? = null

fun foo(pFoo: Boolean, s: String) {
    val local = true
    foo(<caret>)
}

// ORDER: pFoo
// ORDER: nullableFoo
// ORDER: nullableFoo
// ORDER: "pFoo, s"
// ORDER: true
// ORDER: false
// ORDER: local
// ORDER: nonNullable
// ORDER: nullableX
// ORDER: nullableX
