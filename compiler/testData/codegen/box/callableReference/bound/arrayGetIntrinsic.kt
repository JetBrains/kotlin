fun box(): String {
    return if ((arrayOf(1, 2, 3)::get)(1) == 2) "OK" else "Fail"
}
