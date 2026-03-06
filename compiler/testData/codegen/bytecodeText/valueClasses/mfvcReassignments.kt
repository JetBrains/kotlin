// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// LANGUAGE: +ValueClasses

@JvmInline
value class DPoint(val x: Double, val y: Double)

class Box(var value: DPoint)

fun supplier(index: Int) {} // to make usage of the argument
fun supplier(index: Int, x: DPoint) {} // to make usage of the argument


fun `1`() = 1.0
fun `2`() = 2.0
fun `3`() = 3.0
fun `4`() = 4.0
fun `5`() = 5.0
fun `6`() = 6.0
fun `7`() = 7.0
fun `8`() = 8.0

fun reassignVariable(x: DPoint, box: Box) {
    supplier(100)
    var p = DPoint(`1`(), `2`()) // should not use temporary variables
    supplier(101, p)
    p = p // should not use temporary variables
    supplier(102, p)
    p = DPoint(`3`(), `4`()) // should use tempVars
    supplier(103, p)
    p = x // should not use temporary variables
    supplier(104, p)
    p = box.value // should use temporary variables
    supplier(105, p)
    p = listOf(p)[0] // should use temporary variables
    supplier(106, p)
}

fun reassignField(x: DPoint, box: Box) {
    supplier(107)
    val p = DPoint(`5`(), `6`())
    supplier(108, p)
    var b = Box(p) // should not use temporary variables
    supplier(109)
    b.value = b.value // should not use temporary variables
    supplier(110)
    b.value = DPoint(`7`(), `8`()) // should use tempVars
    supplier(111)
    b.value = x // should not use temporary variables
    supplier(112)
    b.value = box.value // should not use temporary variables
    supplier(113)
    b.value = listOf(p)[0] // should use temporary variables
    supplier(114)
}

// 1 100(\D|\d\D|\d\d\D)*(DSTORE(\D|\d\D|\d\d\D)*){2}101
// 0 100(\D|\d\D|\d\d\D)*(DSTORE(\D|\d\D|\d\d\D)*){3}101
// 1 101(\D|\d\D|\d\d\D)*(DSTORE(\D|\d\D|\d\d\D)*){2}102
// 0 101(\D|\d\D|\d\d\D)*(DSTORE(\D|\d\D|\d\d\D)*){3}102
// 1 102(\D|\d\D|\d\d\D)*(DSTORE(\D|\d\D|\d\d\D)*){4}103
// 0 102(\D|\d\D|\d\d\D)*(DSTORE(\D|\d\D|\d\d\D)*){5}103
// 1 103(\D|\d\D|\d\d\D)*(DSTORE(\D|\d\D|\d\d\D)*){2}104
// 0 103(\D|\d\D|\d\d\D)*(DSTORE(\D|\d\D|\d\d\D)*){3}104
// 1 104(\D|\d\D|\d\d\D)*(DSTORE(\D|\d\D|\d\d\D)*){4}105
// 0 104(\D|\d\D|\d\d\D)*(DSTORE(\D|\d\D|\d\d\D)*){5}105
// 1 105(\D|\d\D|\d\d\D)*(DSTORE(\D|\d\D|\d\d\D)*){4}106
// 0 105(\D|\d\D|\d\d\D)*(DSTORE(\D|\d\D|\d\d\D)*){5}106

// 1 107(\D|\d\D|\d\d\D)*(DSTORE(\D|\d\D|\d\d\D)*){2}108
// 0 107(\D|\d\D|\d\d\D)*(DSTORE(\D|\d\D|\d\d\D)*){3}108
// 0 108(\D|\d\D|\d\d\D)*(DSTORE(\D|\d\D|\d\d\D)*){1}109
// 0 109(\D|\d\D|\d\d\D)*(DSTORE(\D|\d\D|\d\d\D)*){1}110
// 0 109(\D|\d\D|\d\d\D)*(ASTORE(\D|\d\D|\d\d\D)*){1}110
// 1 110(\D|\d\D|\d\d\D)*(DSTORE(\D|\d\D|\d\d\D)*){2}111
// 0 110(\D|\d\D|\d\d\D)*(DSTORE(\D|\d\D|\d\d\D)*){3}111
// 0 111(\D|\d\D|\d\d\D)*(DSTORE(\D|\d\D|\d\d\D)*){1}112
// 0 112(\D|\d\D|\d\d\D)*(DSTORE(\D|\d\D|\d\d\D)*){1}113
// 0 112(\D|\d\D|\d\d\D)*(ASTORE(\D|\d\D|\d\d\D)*){1}113
// 1 113(\D|\d\D|\d\d\D)*(ASTORE(\D|\d\D|\d\d\D)*){1}114
// 0 113(\D|\d\D|\d\d\D)*(ASTORE(\D|\d\D|\d\d\D)*){2}114
