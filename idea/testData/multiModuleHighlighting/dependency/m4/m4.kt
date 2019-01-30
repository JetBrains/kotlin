package foo

public fun accessM4() {
    accessM1()
    accessM2()
    accessM3()
    accessM4()
}

open class W {
    fun baz() {}
}

class C : D()

fun main() {
    C().baz()
    bar().baz()
}
