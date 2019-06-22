// TARGET_BACKEND: JVM_IR
fun box(): String {
    for (i in 1..5 step 2 step 1) {
    }

    return "OK"
}

// For "step" progressions in JVM IR, a call to getProgressionLastElement() is made to compute the "last" value. This is normally done for
// each nested call to "step", except when the step value is constant and == 1.
// If the step is non-constant, there is a check that it is > 0, and if not, an IllegalArgumentException is thrown. However, when the
// step is constant and > 0, this check does not need to be added.
//
// Expected lowered form of loop:
//
//   // Additional variables:
//   val innerNestedLast = getProgressionLastElement(1, 5, 2)
//
//   // Standard form of loop over progression
//   var inductionVar = 1
//   val last = innerNestedLast
//   val step = 1
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
// 0 NEW java/lang/IllegalArgumentException
// 0 ATHROW
// 1 IF_ICMPGT
// 1 IF_ICMPNE
// 2 IF