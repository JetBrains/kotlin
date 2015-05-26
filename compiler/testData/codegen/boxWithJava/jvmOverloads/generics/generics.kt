class C<T> {
    [kotlin.jvm.jvmOverloads] public fun foo(o: T, k: String = "K"): String = o.toString() + k
}

fun box(): String {
    return Test.invokeMethodWithOverloads()
}
