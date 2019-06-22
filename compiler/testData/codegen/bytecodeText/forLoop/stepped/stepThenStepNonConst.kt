// TARGET_BACKEND: JVM_IR
fun one() = 1

fun box(): String {
    for (i in 1..6 step one() step one()) {
    }

    return "OK"
}

// For "step" progressions in JVM IR, if the step is non-constant, there is a check that it is > 0, and if not, an IllegalArgumentException
// is thrown.
//
// Expected lowered form of loop:
//
//   // Additional variables:
//   val innerStepArg = one()
//   val innerNewStep = if (innerStepArg > 0) innerStepArg
//                      else throw IllegalArgumentException("Step must be positive, was: $innerStepArg.")
//   val outerNestedLast = getProgressionLastElement(1, 6, innerNewStep)
//   val outerStepArg = one()
//   val outerNewStep = if (outerStepArg > 0) outerStepArg
//                      else throw IllegalArgumentException("Step must be positive, was: $outerStepArg.")
//
//   // Standard form of loop over progression
//   var inductionVar = 1
//   val last = getProgressionLastElement(1, outerNestedLast, outerNewStep)
//   val step = outerNewStep
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
// 2 INVOKESTATIC kotlin/internal/ProgressionUtilKt.getProgressionLastElement
// 2 NEW java/lang/IllegalArgumentException
// 2 ATHROW
// 1 IF_ICMPGT
// 1 IF_ICMPNE
// 2 IFLE
// 4 IF