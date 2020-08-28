// "Add parameter to function 'baz'" "true"
fun bar(): Int = 42

fun baz() {}

fun foo() {
    baz(::bar<caret>)
}