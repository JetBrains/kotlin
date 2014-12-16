fun foo(f: (Int) -> Unit) {
    { (m: Int, n: Int, s: String) ->
        val a = n + m
        println(s)
        f(a)
    }

    { (n: Int, s: String) ->
        val a = n + 1
        println(s)
        f(a)
    }

    <selection>{ Int.(n: Int, s: String) ->
        val a = n + this
        println(s)
        f(a)
    }</selection>

    { Int.(m: Int, r: String) ->
        val b = m + this
        println(r)
        f(b)
    }

    val g: Int.(Int, String) -> Unit = { (a, b) ->
        val m = a + this
        println(b)
        f(m)
    }
}