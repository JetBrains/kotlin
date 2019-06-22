// TARGET_BACKEND: JVM_IR
fun box(): String {
    for (i in 1..6 step 0) {
    }

    return "OK"
}

// For "step" progressions in JVM IR, if the step is constant and <= 0, the expression for step is replaced with an
// IllegalArgumentException. The backend can then eliminate the entire loop and the rest of the function as dead code.
//
// Expected lowered form of loop (before bytecode optimization):
//
//   // Additional variables:
//   val newStep = throw IllegalArgumentException("Step must be positive, was: 0.")
//
//   // Standard form of loop over progression
//   var inductionVar = 1
//   val last = getProgressionLastElement(1, 6, newStep)
//   val step = newStep
//   if (inductionVar <= last) {
//     // Loop is not empty
//     do {
//       val i = inductionVar
//       inductionVar += step
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