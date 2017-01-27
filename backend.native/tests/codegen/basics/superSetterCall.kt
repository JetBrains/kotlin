open class C {
    open var p2 = "<prop:C>"
        set(value)  { field = "<prop:C>" + value }
}

class C1: C() {
    override var p2 = super<C>.p2 + "<prop:C1>"
        set(value) {
            super<C>.p2 = value
            field = "<prop:C1>" + super<C>.p2
        }
}

open class C2: C() {
}

class C3: C2() {
    override var p2 = super<C2>.p2 + "<prop:C3>"
        set(value) {
            super<C2>.p2 = value
            field = "<prop:C3>" + super<C2>.p2
        }
}

fun main(args: Array<String>) {
    val c1 = C1()
    val c3 = C3()
    c1.p2 = "zzz"
    c3.p2 = "zzz"
    println(c1.p2)
    println(c3.p2)
}