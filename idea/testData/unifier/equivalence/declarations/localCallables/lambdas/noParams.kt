fun foo(f: (Int) -> Unit) {
    <selection>{
        val a = 1
        f(a)
    }</selection>

    {
        val b = 1
        f(b)
    }

    val g: () -> Unit = {
        val c = 1
        f(c)
    }
}