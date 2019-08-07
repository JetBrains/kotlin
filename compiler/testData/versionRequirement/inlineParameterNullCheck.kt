package test

inline fun doRun(f: () -> Unit) {}

// Note that although lambdas are not inlined in property accessors (neither in setter parameter, nor in extension receiver parameter),
// we still generate version requirements, just in case we support inlining here in the future.
inline var lambdaVarProperty: () -> Unit
    get() = {}
    set(value) { value() }

inline var (() -> String).extensionProperty: String
    get() = this()
    set(value) { this() }
