val foo = fun(a: Int): String {
    if (a == 1) return "4"
    when (a) {
        5 -> return "2"
        3 -> return null
        2 -> return 2
    }
    return ""
}