fun box(): String {
    var v = "FAIL"
    val max = JavaClass.findMaxAndInvokeCallback({ a, b -> a.length - b.length }, "foo", "kotlin", { v = "OK" })
    if (max != "kotlin") return "Wrong max: $max"
    return v
}