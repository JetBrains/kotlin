interface I1

interface I2 {
    override fun toString(): String
}

interface I3 : I1, I2

class I3Impl : I3 {
    override fun toString() = "OK"
}

fun foo(i3: I3) = i3.toString()

fun box() = foo(I3Impl())