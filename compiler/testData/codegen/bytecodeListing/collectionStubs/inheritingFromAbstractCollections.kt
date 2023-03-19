// WITH_STDLIB

// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-57300 K2: subclass of MutableCollection with primitive element type has methods with boxed type

abstract class AIterD : AbstractIterator<Double>()

abstract class ACollD : AbstractCollection<Double>()

abstract class AMCollD : AbstractMutableCollection<Double>()

abstract class ASetD : AbstractSet<Double>()

abstract class AMSetD : AbstractMutableSet<Double>()

abstract class AListD : AbstractList<Double>()
