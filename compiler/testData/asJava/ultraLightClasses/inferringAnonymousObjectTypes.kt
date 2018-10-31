/** should load cls */
class Prop {
    private val someProp = object { }
}


/** should load cls */
class Fun {
    private fun someFun() = object { }
}

/** should load cls */
class Array {
    val a1 = arrayOf(
        object { val fy = "text"}
    )
}

/** should load cls */
private class C(val y: Int) {
    val initChild = { ->
        object {
            override fun toString(): String {
                return "child" + y
            }
        }
    }
}


class Super {
    val a: Any?
}

/** should load cls */
class Sub {
    override val a = arrayOf(
        object { val fy = "text"}
    )

}