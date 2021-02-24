// TARGET_BACKEND: JVM_IR
// WITH_RUNTIME
fun box(): String {
    for (i in 1u..6u step 0) {
    }

    return "OK"
}

// For "step" progressions in JVM IR, if the step is constant and <= 0, the expression for step is replaced with an
// IllegalArgumentException. The backend can then eliminate the entire loop and the rest of the function as dead code.
//
// Expected lowered form of loop (before bytecode optimization):
//
//   // Additional statements:
//   throw IllegalArgumentException("Step must be positive, was: 0.")
//
//   // Standard form of loop over progression
//   var inductionVar = 1u
//   val last = getProgressionLastElement(1u, 6u, 0)
//   if (inductionVar <= last) {
//     // Loop is not empty
//     do {
//       val i = inductionVar
//       inductionVar += 0
//       // Loop body
//     } while (i != last)
//   }

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 0 INVOKESTATIC kotlin/internal/ProgressionUtilKt.getProgressionLastElement
// 1 NEW java/lang/IllegalArgumentException
// 1 ATHROW
// 0 IF
// 0 ARETURN
// 0 INVOKESTATIC kotlin/UInt.constructor-impl
// 0 INVOKE\w+ kotlin/UInt.(un)?box-impl
