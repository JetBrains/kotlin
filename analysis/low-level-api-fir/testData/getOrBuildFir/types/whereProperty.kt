interface One
interface Two

val <T> T.foo where T : One, T : <expr>Two</expr> get() = this