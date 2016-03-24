fun foo(p: Int) {
    val v = <caret>"line 1\n" +
            "line 2: " + p + "\n" +
            "line 3"
}
