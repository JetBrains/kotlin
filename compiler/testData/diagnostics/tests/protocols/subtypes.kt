
interface A
class B : A

protocol interface ProtoA {
    val field: A
}

protocol interface ProtoB {
    val field: B
}

class ImplA {
    val field = object: A { }
}

class ImplB {
    val field = B()
}

fun printFieldA(arg: ProtoA) {
    arg.field
}

fun printFieldB(arg: ProtoB) {
    arg.field
}

fun test() {
    printFieldA(ImplA())
    printFieldA(ImplB())
    printFieldB(<!TYPE_MISMATCH!>ImplA()<!>)
    printFieldB(ImplB())
}
