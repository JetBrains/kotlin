class C(@JvmField val foo: String) {

}

fun box(): String {
    return Test.invokeMethodWithPublicField()
}
