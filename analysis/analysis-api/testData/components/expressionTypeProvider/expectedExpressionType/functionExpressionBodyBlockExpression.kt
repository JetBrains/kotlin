val lam4 = fun(a: Int): String <caret>{
    if (a < 5) return "5"

    if (a > 0)
        return "1"
    else
        return "2"
}
