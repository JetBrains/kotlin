open class Base(val callback: () -> String)

class Outer {
    val ok = "OK"

    inner class Inner : Base(
        {
            val lambda = { ok }
            lambda()
        }
    )
}

fun box(): String =
        Outer().Inner().callback()
