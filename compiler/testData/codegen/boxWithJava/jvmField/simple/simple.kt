class C {
    @JvmField public val foo: String = "OK"
}

fun box(): String {
    return Test.invokeMethodWithPublicField()
}
