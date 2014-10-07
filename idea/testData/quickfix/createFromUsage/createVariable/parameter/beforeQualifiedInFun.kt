// "Create parameter 'foo'" "false"
// ACTION: Split property declaration
// ACTION: Create property 'foo' from usage
// ERROR: Unresolved reference: foo

class A

fun test(a: A) {
    val t: Int = a.<caret>foo
}