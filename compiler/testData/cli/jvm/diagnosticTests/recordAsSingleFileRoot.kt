// This should compile.
fun create(): Record = Record("OK")

// This should be an error.
fun error(): Unresolved? = null
