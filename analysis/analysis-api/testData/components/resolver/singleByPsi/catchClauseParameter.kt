fun f(x: Throwable) {
    try {}
    catch (x: Throwable) {
        val x = <expr>x</expr>
    }
}

// IGNORE_LOOKUP_LOCALLY
// ISSUE: KT-89149
