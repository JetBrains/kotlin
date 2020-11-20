// PROBLEM: none
class A

fun test(a: A) {
    a.(fun <caret>A.() {
    })()
}