// LAMBDAS: CLASS

fun test() {
    {
        val lam = {}
        lam()
    }()
}

inline fun ifun(s: () -> Unit) {
    s()
}

fun test2() {
    var z = 1;
    ifun {
        val lam = { z = 2 }
        lam()
    }
}

// 3 final class
// 1 class DeleteClassOnTransformationKt\$test\$1\$lam\$1
// 1 class DeleteClassOnTransformationKt\$test2\$1\$lam\$1
