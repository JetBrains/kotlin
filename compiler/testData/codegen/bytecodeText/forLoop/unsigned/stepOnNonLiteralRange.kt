// TARGET_BACKEND: JVM_IR
// WITH_RUNTIME
fun box(): String {
    val uintRange = 1u..7u
    for (i in uintRange step 2) {
    }

    return "OK"
}

// For "step" progressions in JVM IR, a call to getProgressionLastElement() is made to compute the "last" value.
// If "step" is called on a non-literal progression, there is a check to see if that progression's step value is < 0.
// However, if the progression is of type *Range (e.g., IntRange) instead of *Progression (e.g., IntProgression), this
// check is not needed since *Range always has step == 1.
//
// Expected lowered form of loop:
//
//   // Additional statements:
//   val nestedFirst = uintRange.first
//   val nestedLast = uintRange.last
//
//   // Standard form of loop over progression
//   var inductionVar = nestedFirst
//   val last = getProgressionLastElement(nestedFirst, nestedLast, 2)
//   if (inductionVar <= last) {
//     // Loop is not empty
//     do {
//       val i = inductionVar
//       inductionVar += 2
//       // Loop body
//     } while (i != last)
//   }

// 0 iterator
// 0 getStart
// 0 getEnd
// 1 getFirst
// 1 getLast
// 0 getStep
// 1 INVOKESTATIC kotlin/internal/UProgressionUtilKt.getProgressionLastElement
// 0 NEW java/lang/IllegalArgumentException
// 0 ATHROW
// 1 INVOKESTATIC kotlin/UnsignedKt.uintCompare
// 1 IFGT
// 1 IF_ICMPNE
// 2 IF
// 0 INEG
// 0 INVOKESTATIC kotlin/UInt.constructor-impl
// 0 INVOKE\w+ kotlin/UInt.(un)?box-impl
