// TARGET_BACKEND: JVM_IR

fun foo() {
    Runnable {
    }.run()
}

// 1 LINENUMBER 4 L0\n +INVOKEDYNAMIC run\(\)Ljava/lang/Runnable