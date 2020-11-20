suspend fun susp() {}

interface I {

    suspend fun problematic() {
            run {
                    susp()
            }
    }
}

// We only test compilation.
fun box() = "OK"
