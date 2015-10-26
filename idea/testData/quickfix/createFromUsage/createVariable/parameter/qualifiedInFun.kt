// "Create parameter 'foo'" "false"
// ACTION: Create extension property 'foo'
// ACTION: Create member property 'foo'
// ACTION: Create property 'foo' as constructor parameter
// ERROR: Unresolved reference: foo

class A

fun test(a: A) {
    val t: Int = a.<caret>foo
}