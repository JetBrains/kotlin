fun test(func: () -> String?) {
    val x = func() ?: ""
}
