class A {
    init {}

    private init {}

    val x = f()
    init {
        x = 1
    }

    val y = f()
    {
        x = 2
    }
    {
        x = 3
    }
}
