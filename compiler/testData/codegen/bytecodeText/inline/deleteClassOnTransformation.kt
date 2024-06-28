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


// JVM_TEMPLATES:
// 1 class DeleteClassOnTransformationKt\$test\$1 extends
// 1 class DeleteClassOnTransformationKt\$test\$1\$lam\$1 extends
// 0 class DeleteClassOnTransformationKt\$test2\$1\$1
// 1 class DeleteClassOnTransformationKt\$test2\$\$inlined\$ifun\$lambda\$1

// JVM_IR_TEMPLATES:
// 3 final class
// 1 class DeleteClassOnTransformationKt\$test\$1\$lam\$1
// 1 class DeleteClassOnTransformationKt\$test2\$1\$lam\$1
