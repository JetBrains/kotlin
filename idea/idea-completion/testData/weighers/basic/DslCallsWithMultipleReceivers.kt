// RUNTIME
package test

class HasFoo {
    fun foo10() {

    }
}

fun main() {
    val foo5 = 3
    with(0) {
        with("") {
            with(HasFoo()) {
                r {
                    foo<caret>
                }
            }
        }
    }
}

@DslMarker
annotation class Dsl


@Dsl
class R

fun r(body: R.() -> Unit) {

}

fun foo1(i: Int) {

}

// ORDER: foo2
// ORDER: foo5
// ORDER: foo10
// ORDER: foo1
