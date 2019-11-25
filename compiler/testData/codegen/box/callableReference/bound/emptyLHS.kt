// IGNORE_BACKEND_FIR: JVM_IR

var result = ""

class A {
    fun memberFunction() { result += "A.mf," }
    fun aMemberFunction() { result += "A.amf," }
    val memberProperty: Int get() = 42.also { result += "A.mp," }
    val aMemberProperty: Int get() = 42.also { result += "A.amp," }

    fun test(): String {
        (::memberFunction)()
        (::aExtensionFunction)()

        (::memberProperty)()
        (::aExtensionProperty)()

        return result
    }

    inner class B {
        fun memberFunction() { result += "B.mf," }
        val memberProperty: Int get() = 42.also { result += "B.mp," }

        fun test(): String {
            (::aMemberFunction)()
            (::aExtensionFunction)()

            (::aMemberProperty)()
            (::aExtensionProperty)()

            (::memberFunction)()
            (::memberProperty)()

            (::bExtensionFunction)()
            (::bExtensionProperty)()

            return result
        }
    }
}

fun A.aExtensionFunction() { result += "A.ef," }
val A.aExtensionProperty: Int get() = 42.also { result += "A.ep," }
fun A.B.bExtensionFunction() { result += "B.ef," }
val A.B.bExtensionProperty: Int get() = 42.also { result += "B.ep," }

fun box(): String {
    val a = A().test()
    if (a != "A.mf,A.ef,A.mp,A.ep,") return "Fail $a"

    result = ""
    val b = A().B().test()
    if (b != "A.amf,A.ef,A.amp,A.ep,B.mf,B.mp,B.ef,B.ep,") return "Fail $b"

    result = ""
    with(A()) {
        (::memberFunction)()
        (::aExtensionFunction)()

        (::memberProperty)()
        (::aExtensionProperty)()
    }
    if (result != "A.mf,A.ef,A.mp,A.ep,") return "Fail $result"

    result = ""
    with(A()) {
        with(B()) {
            (::aMemberFunction)()
            (::aExtensionFunction)()

            (::aMemberProperty)()
            (::aExtensionProperty)()

            (::memberFunction)()
            (::memberProperty)()

            (::bExtensionFunction)()
            (::bExtensionProperty)()
        }
    }
    if (result != "A.amf,A.ef,A.amp,A.ep,B.mf,B.mp,B.ef,B.ep,") return "Fail $result"

    return "OK"
}
