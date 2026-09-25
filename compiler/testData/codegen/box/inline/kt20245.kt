// ISSUE: KT-20245

// Case 1
interface ParentInterface {
    val child: ChildInterface
}
interface ChildInterface {
    val field: String
}
fun crash1(data: String): ParentInterface =
    Unit.let {
        object : ParentInterface {
            override val child = object : ChildInterface {
                override val field = data
            }
        }
    }

// Case 2
interface HasO {
    val o: Any
}

inline fun crash2(crossinline b: () -> String) =
    object : HasO {
        override val o = object {
            override fun toString() = b()
        }
    }

fun box(): String {
    if (crash1("OK").child.field != "OK") return "fail: 1"
    if (crash2 { "OK" }.o.toString() != "OK") return "fail: 2"
    return "OK"
}
