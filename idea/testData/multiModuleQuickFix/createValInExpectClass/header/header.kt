// "Create member property 'Foo.foo'" "true"
// ERROR: Unresolved reference: foo

expect class Foo

fun test(f: Foo) {
    takeInt(f.<caret>foo)
}

fun takeInt(n: Int) {

}