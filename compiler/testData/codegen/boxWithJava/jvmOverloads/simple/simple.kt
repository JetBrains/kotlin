class C {
    [kotlin.jvm.overloads] public fun foo(o: String = "O", k: String = "K"): String = o + k
}

fun box(): String {
    return Test.invokeMethodWithOverloads()
}
