class C<T> {
    @kotlin.jvm.JvmOverloads public fun foo(o: T, k: String = "K"): String = o.toString() + k
}

fun box(): String {
    return Test.invokeMethodWithOverloads()
}
