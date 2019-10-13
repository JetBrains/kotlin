class GetTestClass<T>(t: T) {
    var value = t
}

fun box(): String {
    val implicitInstance: GetTestClass<Int> = GetTestClass<Int>(1)
    val explicitInstance = GetTestClass(1)
    if (implicitInstance.value == explicitInstance.value) {
        return "OK"
    } else {
        return "FAIL"
    }
}

