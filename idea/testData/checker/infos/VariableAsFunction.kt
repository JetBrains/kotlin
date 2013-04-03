class FunctionLike {
    fun invoke() {
    }
}

val <info>property</info> = {}

fun foo(param: (Int) -> Int, functionLike: FunctionLike) {
    <info descr="Calling parameter as function">param</info>(1)
    <info descr="Calling parameter as function-like">functionLike</info>()

    val v1 = param
    var v2 = param

    <info descr="Calling variable as function">v1</info>(1)
    <info descr="Calling variable as function">v2</info>(1)

    <info descr="Calling property as function">property</info>();

    {}() //should not be highlighted as "calling variable as function"
}