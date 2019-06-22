// TARGET_BACKEND: JVM_IR
fun nine() = 9

fun box(): String {
    for (i in 1 until nine() step 2) {
    }

    return "OK"
}

// For "until" progressions in JVM IR, there is a check that the range is not empty: upper bound != MIN_VALUE.
//
// Expected lowered form of loop (before bytecode optimizations):
//
//   // Additional variables:
//   val untilArg = nine()
//   val nestedLast = untilArg - 1
//
//   // Standard form of loop over progression
//   var inductionVar = 1
//   val last = getProgressionLastElement(1, nestedLast, 2)
//   val step = 2
//   if (untilArg != Int.MIN_VALUE && inductionVar <= last) {
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
// 1 LDC -2147483648
// 1 IF_ICMPEQ
// 1 IF_ICMPGT
// 1 IF_ICMPNE
// 3 IF