// TARGET_BACKEND: JVM_IR
fun box(): String {
    val intProgression = 1..7
    for (i in intProgression step 2) {
    }

    return "OK"
}

// For "step" progressions in JVM IR, a call to getProgressionLastElement() is made to compute the "last" value.
// If "step" is called on a non-literal progression, there is a check to see if that progression's step value is < 0.
// If the step is non-constant, there is a check that it is > 0, and if not, an IllegalArgumentException is thrown. However, when the
// step is constant and > 0, this check does not need to be added.
//
// Expected lowered form of loop:
//
//   // Additional variables:
//   val progression = intProgression
//   val nestedFirst = progression.first
//   val nestedLast = progression.last
//   val nestedStep = progression.step
//   val newStep = if (nestedStep > 0) 2 else -2
//
//   // Standard form of loop over progression
//   var inductionVar = nestedFirst
//   val last = getProgressionLastElement(nestedFirst, nestedLast, newStep)
//   val step = newStep
//   if ((step > 0 && inductionVar <= last) || (step < 0 && last <= inductionVar)) {
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
// 1 getFirst
// 1 getLast
// 1 getStep
// 1 INVOKESTATIC kotlin/internal/ProgressionUtilKt.getProgressionLastElement
// 0 NEW java/lang/IllegalArgumentException
// 0 ATHROW
// 1 IF_ICMPGT
// 1 IF_ICMPLE
// 1 IF_ICMPNE
// 2 IFLE
// 1 IFGE
// 6 IF
// 0 INEG