package aaa

fun <T> T.foo(t: T) = t

fun id<T>(t: T) = t

fun a() {
    val i = id(2 foo 3)
    i : Int // i shouldn't be resolved to error element
}
