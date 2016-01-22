import test.*

fun box(): String {
    val res = call {
        { "OK" }
    }

    var enclosingMethod = res.javaClass.enclosingMethod
    if (enclosingMethod?.name != "box") return "fail 1: ${enclosingMethod?.name}"

    var enclosingClass = res.javaClass.enclosingClass
    if (enclosingClass?.name != "AnonymousInLambda_1Kt") return "fail 2: ${enclosingClass?.name}"

    return res()
}
