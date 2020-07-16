// TARGET_BACKEND: JVM_IR
// WITH_RUNTIME
fun nine() = 9u

fun box(): String {
    for (i in 1u until nine() step 2) {
    }

    return "OK"
}

// For "until" progressions in JVM IR, there is a check that the range is not empty: upper bound != MIN_VALUE.
//
// Expected lowered form of loop (before bytecode optimizations):
//
//   // Additional statements:
//   val untilArg = nine()
//   val nestedLast = untilArg - 1u
//
//   // Standard form of loop over progression
//   var inductionVar = 1
//   val last = getProgressionLastElement(1u, nestedLast, 2)
//   if (untilArg != UInt.MIN_VALUE && inductionVar <= last) {
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
// 1 INVOKESTATIC kotlin/internal/UProgressionUtilKt.getProgressionLastElement
// 0 NEW java/lang/IllegalArgumentException
// 1 INVOKESTATIC kotlin/UnsignedKt.uintCompare
// 1 IFEQ
// 1 IFGT
// 1 IF_ICMPNE
// 3 IF
// 0 INVOKESTATIC kotlin/UInt.constructor-impl
// 0 INVOKE\w+ kotlin/UInt.(un)?box-impl
