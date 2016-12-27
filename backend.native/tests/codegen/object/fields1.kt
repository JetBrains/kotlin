class B(val a:Int, b:Int) {
    constructor(pos:Int):this(1, pos) {}
    val pos = b + 1
}

fun primaryConstructorCall(a:Int, b:Int) = B(a, b).pos

fun secondaryConstructorCall(a:Int) = B(a).pos

fun main(args:Array<String>) {
    if (primaryConstructorCall(0xdeadbeef.toInt(), 41) != 42) throw Error()
    if (secondaryConstructorCall(41)                   != 42) throw Error()
}