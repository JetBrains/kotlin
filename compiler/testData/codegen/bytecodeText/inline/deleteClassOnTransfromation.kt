// IGNORE_BACKEND: JVM_IR
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

// 1 class DeleteClassOnTransfromationKt\$test\$1\$1
// 0 class DeleteClassOnTransfromationKt\$test2\$1\$1