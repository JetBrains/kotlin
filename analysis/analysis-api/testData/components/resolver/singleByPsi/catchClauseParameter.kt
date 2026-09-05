fun f(x: Throwable) {
    try {}
    catch (x: Throwable) {
        val x = <expr>x</expr>
    }
}

// ISSUE: KT-89149
