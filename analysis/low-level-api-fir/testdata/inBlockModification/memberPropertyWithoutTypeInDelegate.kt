class A {
    val <caret>x by lazy {
        fun doSmth(i: String) = 4
        doSmth("str")
    }
}
