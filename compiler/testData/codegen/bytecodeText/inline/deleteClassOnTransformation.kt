fun test() {
    {
        {}()
    }()
}

inline fun ifun(s: () -> Unit) {
    s()
}

fun test2() {
    var z = 1;
    ifun {
        { z = 2 }()
    }
}

// 1 class DeleteClassOnTransformationKt\$test\$1\$1

// JVM_TEMPLATES:
// 0 class DeleteClassOnTransformationKt\$test2\$1\$1
// 1 class DeleteClassOnTransformationKt\$test2\$\$inlined\$ifun\$lambda\$1

// JVM_IR_TEMPLATES:
// 1 class DeleteClassOnTransformationKt\$test2\$1\$1
// 0 class DeleteClassOnTransformationKt\$test2\$\$inlined
