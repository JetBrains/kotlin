inline fun (Int.() -> String).foo(): String {
    return noInlineRun(this)
}

inline var (Int.() -> String).bar: String
    get() = noInlineRun(this)
    set(value) {
        noInlineRun(this)
    }

fun noInlineRun(f: Int.() -> String): String { return f(1) }

fun box() = { a: Int -> if (a == 1) "O" else "FA" }.foo() + { a: Int -> if (a == 1) "K" else "IL" }.bar