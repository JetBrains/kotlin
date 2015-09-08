@inline @tailrec class A {
    @inline(1) fun foo() {

    }

    kotlin.inline fun bar() {
        @kotlin.data() class Local
    }
}
