class A

fun foo(x: Any?) {}

fun box() {
    val x: Int? = 1
    x!!

    val z: Int? = if (1 == 1) x else null
    z!!

    foo(1 as java.lang.Integer)

    val y: Any? = if (1 == 1) x else A()
    y!!
}

// 0 IFNULL
// 0 IFNONULL
// 0 throwNpe
// 0 ATHROW

// JVM_TEMPLATES:
// 1 checkNotNull \(Ljava/lang/Object;\)V

// JVM_IR_TEMPLATES:
// 0 checkNotNull \(Ljava/lang/Object;\)V
