// FLOW: OUT

fun f1(param: String) {}

fun f4(list: List<String>) {
    for (<caret>s in list)
        if (s.length == 0)
            f1(s)
}