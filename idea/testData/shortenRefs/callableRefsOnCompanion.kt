package test

class A {
    companion object {
        fun foo() {

        }
    }
}

fun A.Companion.bar() {

}

fun test() {
    <selection>A.Companion::foo
    A.Companion::bar
    (A.Companion)::foo
    (A.Companion)::bar</selection>
}