fun foo(body: (Int) -> Unit) = body(1)

fun test() {
    foo {
        val x = <info descr="null">~it</info> + 1
        val xx = <info descr="null">it</info> + 2
        foo {
            val y = it - 1
            val yy = it - 2
        }
        val xxx = <info descr="null">it</info> + 3
    }
}