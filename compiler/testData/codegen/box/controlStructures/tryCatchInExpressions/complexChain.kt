// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
fun cleanup() {}

inline fun concat(x: String, y: String): String = x + y

inline fun throws() {
    try {
        throw Exception()
    }
    finally {
        cleanup()
    }
}

inline fun first(x: String, y: String): String = x

fun box(): String =
    "" + concat(
            try { "" } finally { "0" },
            "" + concat(
                    first(
                            try { 
                                try { 
                                    "O" 
                                } 
                                finally { 
                                    "1"
                                } 
                            } 
                            catch (e: Exception) { 
                                throw e 
                            }
                            finally {
                                cleanup()
                            }, 
                            "2"
                    ),
                    first(
                        try { 
                            throws()
                            throw Exception()
                            "3"
                        } 
                        catch (e: Exception) { 
                            "K" 
                        } 
                        finally {
                            cleanup()
                        },
                        "4"
                    )
            )
    )
