open class C {
    open val p1 = "<prop:C>"
}

class C1: C() {
    override val p1 = super<C>.p1 + "<prop:C1>"
}

open class C2: C() {
}

class C3: C2() {
    override val p1 = super<C2>.p1 + "<prop:C3>"
}

fun main(args: Array<String>) {
    println(C1().p1)
    println(C3().p1)
}