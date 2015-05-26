class C {
    [kotlin.jvm.jvmOverloads] public fun foo(o: String = "O", k: String = "K"): String = o + k
}

fun box(): String {
    return Test.invokeMethodWithOverloads()
}
