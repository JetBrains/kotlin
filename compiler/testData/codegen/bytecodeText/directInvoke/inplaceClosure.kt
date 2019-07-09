// IGNORE_BACKEND: JVM_IR
fun test() {
    1.(fun Int.() = 2)()
}

// 1 invoke \(I\)I