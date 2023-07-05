class A {
    val <caret>: Int = run {
        fun doSmth(i: String) = 4
        doSmth("str")
    }
}
