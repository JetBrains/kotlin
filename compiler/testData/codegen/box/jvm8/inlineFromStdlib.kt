// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
//WITH_RUNTIME

fun box(): String {
    val list = listOf("O", "K")
    return list.fold("") {a, b -> a +b}
}

