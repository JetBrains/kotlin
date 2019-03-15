// IGNORE_BACKEND: JVM_IR
fun test() {
    lateinit var z: String
    run {
        z = ""
    }
    println(z)
}

// 0 IFNULL
// 0 IFNONNULL
