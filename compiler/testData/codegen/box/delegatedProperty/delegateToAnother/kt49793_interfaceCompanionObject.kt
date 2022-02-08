// WITH_STDLIB

class Z(var x: String = "Fail")

operator fun Z.getValue(x: Any?, y: Any?): Z = this
operator fun Z.setValue(x: Any?, y: Any?, value: Z) { this.x = value.x }

interface O {
    companion object {
        val instance: Z by Z()
        var y by instance::x
    }
}

fun box(): String {
    O.y = "OK"
    return O.y
}
