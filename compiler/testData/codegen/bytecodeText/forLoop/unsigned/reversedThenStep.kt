// TARGET_BACKEND: JVM_IR
// WITH_RUNTIME
fun box(): String {
    for (i in (1u..8u).reversed() step 2) {
    }

    return "OK"
}

// For "step" progressions in JVM IR, a call to getProgressionLastElement() is made to compute the "last" value.
// If the step is non-constant, there is a check that it is > 0, and if not, an IllegalArgumentException is thrown. However, when the
// step is constant and > 0, this check does not need to be added.
//
// Expected lowered form of loop:
//
//   // Standard form of loop over progression
//   val last = getProgressionLastElement(8u, 1u, -2)
//   var inductionVar = 8u
//   if (last <= inductionVar) {
//     // Loop is not empty
//     do {
//       val i = inductionVar
//       inductionVar += -2
//       // Loop body
//     } while (i != last)
//   }
//
// We can't use "0 reversed" in the regex below because "reversed" is in the test filename.

// 0 INVOKE.*reversed
// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 1 INVOKESTATIC kotlin/internal/UProgressionUtilKt.getProgressionLastElement
// 0 NEW java/lang/IllegalArgumentException
// 0 ATHROW
// 1 IFGT
// 1 IF_ICMPNE
// 2 IF
// 0 INVOKESTATIC kotlin/UInt.constructor-impl
// 0 INVOKE\w+ kotlin/UInt.(un)?box-impl
