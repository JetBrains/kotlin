// IGNORE_BACKEND_FIR: JVM_IR
fun test() {
    1.(fun Int.() = 2)()
}

// 1 invoke \(I\)I