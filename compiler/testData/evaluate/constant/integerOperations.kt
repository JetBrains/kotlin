package test

// val x1: 3
val x1 = 1 + 2

// val x2: 3.toLong()
val x2 = 1 + 2L

// val x3: 3
val x3 = 1.toShort() + 2.toByte()

// val x4: 3
val x4 = 1.toByte() + 2.toByte()

// val x5: 4656
val x5 = 0x1234 and 0x5678

// Strange result, see KT-13517
// val x6: null
val x6 = 0x1234 and 0x5678L

// val x7: 4656.toLong()
val x7 = 0x1234L and 0x5678