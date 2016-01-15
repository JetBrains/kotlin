import test.*

fun box(): String {
    val res = call {
        test { "OK" }
    }

    var enclosingMethod = res.javaClass.enclosingMethod
    if (enclosingMethod?.name != "box") return "fail 1: ${enclosingMethod?.name}"

    var enclosingClass = res.javaClass.enclosingClass
    if (enclosingClass?.name != "InlineChain_1Kt") return "fail 2: ${enclosingClass?.name}"

    val res2 = call {
        call {
            test { "OK" }
        }
    }

    enclosingMethod = res2.javaClass.enclosingMethod
    if (enclosingMethod?.name != "box") return "fail 1: ${enclosingMethod?.name}"

    enclosingClass = res2.javaClass.enclosingClass
    if (enclosingClass?.name != "InlineChain_1Kt") return "fail 2: ${enclosingClass?.name}"

    return res2()
}
