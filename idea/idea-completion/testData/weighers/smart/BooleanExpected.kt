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
// ORDER: "pFoo = true"
// ORDER: "pFoo = false"
// ORDER: local
// ORDER: nonNullable
// ORDER: maxOf
// ORDER: maxOf
// ORDER: minOf
// ORDER: minOf
// ORDER: nullableX
// ORDER: nullableX
