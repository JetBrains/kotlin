// IGNORE_BACKEND: JVM_IR
fun f() {
    for (i in 0..5 step 2) {
    }

    for (i in 5 downTo 1 step 1) { // suppress optimized code generation for 'for-in-downTo'
    }
}

// 0 iterator
// 2 getFirst
// 2 getLast
// 2 getStep