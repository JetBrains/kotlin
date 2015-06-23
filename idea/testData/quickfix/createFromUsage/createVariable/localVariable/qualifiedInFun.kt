// "Create local variable 'foo'" "false"
// ACTION: Create extension property 'foo'
// ACTION: Create member property 'foo'
// ERROR: Unresolved reference: foo

class A

fun test(a: A) {
    val t: Int = a.<caret>foo
}