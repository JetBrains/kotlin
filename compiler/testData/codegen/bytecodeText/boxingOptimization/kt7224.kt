// IGNORE_BACKEND_FIR: JVM_IR

fun box(): String {
    230?.hashCode()

    return "OK"
}

// 0 INVOKESTATIC java/lang/Integer.valueOf
