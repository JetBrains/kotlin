class A {
    val x by lazy {
        fun doSmth(i: String) = 4
        <expr>doSmth</expr>("str")
    }
}
