fun almostAlwaysTrue() = true

fun runNoInline(f: () -> Unit) = f()

fun test() {
    lateinit var z: String

    runNoInline {
        // NB this code can be executed in a different thread multiple times, each time with different results.
        // So, 'z' can be initialized at any moment, and should be checked on every read.

        if (almostAlwaysTrue()) {
            z = ""
        }
    }

    println(z)
    println(z)
    println(z)
}

// 0 IFNULL
// 3 IFNONNULL
// 3 throwUninitializedPropertyAccessException
