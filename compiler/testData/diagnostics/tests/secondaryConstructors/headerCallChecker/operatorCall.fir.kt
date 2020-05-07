open class C(val x: Int)

class D : C {
    constructor() : super(
            {
                val s = ""
                s()
                ""()
                42
            }())

    operator fun String.invoke() { }
}
