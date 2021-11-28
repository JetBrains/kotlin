// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
fun box(): String {
    val uintProgression = 1u..7u step 3  // `step` ensures type is UIntProgression, NOT UIntRange
    for (i in uintProgression step 2) {
    }

    return "OK"
}

// For "step" progressions in JVM IR, a call to getProgressionLastElement() is made to compute the "last" value.
// If "step" is called on a non-literal progression, there is a check to see if that progression's step value is < 0.
// If the step is non-constant, there is a check that it is > 0, and if not, an IllegalArgumentException is thrown. However, when the
// step is constant and > 0, this check does not need to be added.
//

// 0 iterator
// 0 getStart
// 0 getEnd
// 1 getFirst
// 1 getLast
// 1 getStep
// 1 INVOKESTATIC kotlin/internal/UProgressionUtilKt.getProgressionLastElement
// 0 NEW java/lang/IllegalArgumentException
// 0 ATHROW
// 2 INVOKESTATIC kotlin/UnsignedKt.uintCompare
// 2 IFGT
// 1 IF_ICMPEQ
// 2 IFLE
// 1 IFGE
// 6 IF
// 0 INEG
// 0 INVOKESTATIC kotlin/UInt.constructor-impl
// 0 INVOKE\w+ kotlin/UInt.(un)?box-impl
// 15 ILOAD
// 7 ISTORE
// 1 IADD
// 0 ISUB
// 0 IINC
