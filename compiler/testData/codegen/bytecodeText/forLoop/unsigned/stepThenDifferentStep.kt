// TARGET_BACKEND: JVM_IR
// WITH_RUNTIME
fun box(): String {
    for (i in 1u..7u step 3 step 2) {
    }

    return "OK"
}

// For "step" progressions in JVM IR, a call to getProgressionLastElement() is made to compute the "last" value. This is done for each
// nested call to "step".
// If the step is non-constant, there is a check that it is > 0, and if not, an IllegalArgumentException is thrown. However, when the
// step is constant and > 0, this check does not need to be added.
//
// Expected lowered form of loop:
//
//   // Additional statements:
//   val outerNestedLast = getProgressionLastElement(1u, 7u, 3)
//
//   // Standard form of loop over progression
//   var inductionVar = 1u
//   val last = getProgressionLastElement(1u, outerNestedLast, 2)
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
// 0 getFirst
// 0 getLast
// 0 getStep
// 2 INVOKESTATIC kotlin/internal/UProgressionUtilKt.getProgressionLastElement
// 0 NEW java/lang/IllegalArgumentException
// 0 ATHROW
// 1 INVOKESTATIC kotlin/UnsignedKt.uintCompare
// 1 IFGT
// 1 IF_ICMPNE
// 2 IF
// 0 INVOKESTATIC kotlin/UInt.constructor-impl
// 0 INVOKE\w+ kotlin/UInt.(un)?box-impl
