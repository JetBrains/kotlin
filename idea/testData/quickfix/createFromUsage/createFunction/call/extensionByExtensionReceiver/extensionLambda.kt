// "Create extension function 'A.foo'" "true"
// WITH_RUNTIME
fun bar(b: Boolean) {

}

class A(val n: Int)

fun test() {
    with(A(1)) {
        bar(<caret>foo(n))
    }
}