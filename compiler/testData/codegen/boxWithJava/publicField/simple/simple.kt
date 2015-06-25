class C {
    @publicField private val foo: String = "OK"
}

fun box(): String {
    return Test.invokeMethodWithPublicField()
}
