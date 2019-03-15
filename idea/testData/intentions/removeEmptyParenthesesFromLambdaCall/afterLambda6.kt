// IS_APPLICABLE: false

object A {
    object B {
        class C {
            fun returnFun(fn: () -> Unit): (() -> Unit) -> Unit = {}
        }
    }
}

fun test() {
    A.B.C().returnFun {} ()<caret> {}
}