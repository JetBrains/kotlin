class Out<out T>

fun foo(x: Out<out Int>): Out<String> {
    return x as Out<String>
}

fun bar(x: Out<in Int>) {}
