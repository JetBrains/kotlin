// IGNORE_BACKEND_FIR: JVM_IR

interface Tr {
   fun extra() : String = "_"
}

class N() : Tr {
   override fun extra() : String = super<Tr>.extra() + super<Tr>.extra()
}

fun box(): String {
    val n = N()
    if (n.extra() == "__") return "OK"
    return "fail";
}
