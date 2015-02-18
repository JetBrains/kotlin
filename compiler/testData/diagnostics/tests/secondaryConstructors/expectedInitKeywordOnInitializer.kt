class A {
    val x: Int
    val y: Int
    <!INIT_KEYWORD_BEFORE_CLASS_INITIALIZER_EXPECTED!>{<!>
    x = 1
    }
    init {
        y = 1
    }
}
