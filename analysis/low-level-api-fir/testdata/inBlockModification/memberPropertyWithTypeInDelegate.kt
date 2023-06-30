class A {
    val <caret>x: Int by lazy {
        fun doSmth(i: String) = 4
        doSmth("str")
    }
}
