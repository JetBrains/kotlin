class A {
    val x<caret>: Int = run {
        fun doSmth(i: String) = 4
        doSmth("str")
    }
}
