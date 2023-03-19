// WITH_STDLIB

// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-57300 K2: subclass of MutableCollection with primitive element type has methods with boxed type

abstract class AMListD : AbstractMutableList<Double>()

abstract class AMListI : AbstractMutableList<Int>()
