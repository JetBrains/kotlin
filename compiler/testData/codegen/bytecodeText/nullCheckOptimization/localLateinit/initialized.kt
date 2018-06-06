// LANGUAGE_VERSION: 1.2

fun test() {
    lateinit var z: String
    run {
        z = ""
    }
    println(z)
}

// 0 IFNULL
// 0 IFNONNULL