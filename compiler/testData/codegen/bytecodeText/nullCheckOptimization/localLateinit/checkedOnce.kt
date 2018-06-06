// LANGUAGE_VERSION: 1.2

fun almostAlwaysTrue() = true

fun test() {
    lateinit var z: String
    run {
        if (almostAlwaysTrue()) {
            z = ""
        }
    }
    println(z)
    println(z)
    println(z)
}

// 0 IFNULL
// 1 IFNONNULL
// 1 throwUninitializedPropertyAccessException