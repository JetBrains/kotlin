import test.*

fun box(): String {
    val res = test {
        "OK"
    }

    val enclosingMethod = res.javaClass.enclosingMethod
    if (enclosingMethod?.name != "box") return "fail 1: ${enclosingMethod?.name}"

    val enclosingClass = res.javaClass.enclosingClass
    if (enclosingClass?.name != "ObjectInInlineFun_1Kt") return "fail 2: ${enclosingClass?.name}"

    return "OK"
}
