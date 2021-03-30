// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR

fun foo() {
    val x: Int? = 6
    val hc = x!!.hashCode()
}

// 1 java/lang/Integer.hashCode \(I\)I
// 0 java/lang/Integer.valueOf
// 0 intValue
