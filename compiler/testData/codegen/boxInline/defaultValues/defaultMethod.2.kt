package test


inline fun <T> simpleFun(arg: String = "O", lambda: (String) -> T): T {
    return lambda(arg)
}


inline fun <T> simpleFunR(lambda: (String) -> T, arg: String = "O"): T {
    return lambda(arg)
}

