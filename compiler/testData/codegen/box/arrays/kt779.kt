// IGNORE_BACKEND_FIR: JVM_IR
val <T> Array<T>.length : Int get() = this.size

fun box() = if(arrayOfNulls<Int>(10).length == 10) "OK" else "fail"
