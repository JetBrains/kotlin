// TARGET_BACKEND: JVM_IR
fun one() = 1

fun box(): String {
    for (i in 1..7 step one()) {
    }

    return "OK"
}

// For "step" progressions in JVM IR, a call to getProgressionLastElement() is made to compute the "last" value.
// If the step is non-constant, there is a check that it is > 0, and if not, an IllegalArgumentException is thrown.
//
// Expected lowered form of loop:
//
//   // Additional variables:
//   val stepArg = one()
//   val newStep = if (stepArg > 0) stepArg
//                 else throw IllegalArgumentException("Step must be positive, was: $stepArg.")
//
//   // Standard form of loop over progression
//   var inductionVar = 1
//   val last = getProgressionLastElement(1, 7, newStep)
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
// 1 INVOKESTATIC kotlin/internal/ProgressionUtilKt.getProgressionLastElement
// 1 NEW java/lang/IllegalArgumentException
// 1 ATHROW
// 1 IF_ICMPGT
// 1 IF_ICMPNE
// 1 IFLE
// 3 IF