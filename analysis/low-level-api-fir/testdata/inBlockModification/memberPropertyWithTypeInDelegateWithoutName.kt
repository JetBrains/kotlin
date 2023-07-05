class A {
    val <caret>: Int by lazy {
        fun doSmth(i: String) = 4
        doSmth("str")
    }
}
