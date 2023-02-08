// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
// TARGET_BACKEND: JS_IR
// `Boolean.equals(Boolean)` will not be evaluated in K1
// IGNORE_BACKEND_K1: NATIVE

const val trueVal = true
const val falseVal = false

const val not1 = trueVal.not()
const val not2 = falseVal.not()

const val and1 = trueVal.and(trueVal)
const val and2 = trueVal.and(falseVal)
const val and3 = falseVal.and(trueVal)
const val and4 = falseVal.and(falseVal)

const val or1 = trueVal.or(trueVal)
const val or2 = trueVal.or(falseVal)
const val or3 = falseVal.or(trueVal)
const val or4 = falseVal.or(falseVal)

const val xor1 = trueVal.xor(trueVal)
const val xor2 = trueVal.xor(falseVal)
const val xor3 = falseVal.xor(trueVal)
const val xor4 = falseVal.xor(falseVal)

const val compareTo1 = trueVal.compareTo(trueVal)
const val compareTo2 = trueVal.compareTo(falseVal)
const val compareTo3 = falseVal.compareTo(trueVal)
const val compareTo4 = falseVal.compareTo(falseVal)

const val equals1 = trueVal == trueVal
const val equals2 = trueVal == falseVal
const val equals3 = falseVal == trueVal
const val equals4 = falseVal == falseVal

const val toString1 = trueVal.toString()
const val toString2 = falseVal.toString()

fun box(): String {
    if (not1 != false)  return "Fail 1.1"
    if (not2 != true)   return "Fail 1.2"

    if (and1 != true)   return "Fail 2.1"
    if (and2 != false)  return "Fail 2.2"
    if (and3 != false)  return "Fail 2.3"
    if (and4 != false)  return "Fail 2.4"

    if (or1 != true)    return "Fail 3.1"
    if (or2 != true)    return "Fail 3.2"
    if (or3 != true)    return "Fail 3.3"
    if (or4 != false)   return "Fail 3.4"

    if (xor1 != false)  return "Fail 4.1"
    if (xor2 != true)   return "Fail 4.2"
    if (xor3 != true)   return "Fail 4.3"
    if (xor4 != false)  return "Fail 4.4"

    if (compareTo1 != 0)    return "Fail 5.1"
    if (compareTo2 != 1)    return "Fail 5.2"
    if (compareTo3 != -1)   return "Fail 5.3"
    if (compareTo4 != 0)    return "Fail 5.4"

    if (equals1 != true)    return "Fail 6.1"
    if (equals2 != false)   return "Fail 6.2"
    if (equals3 != false)   return "Fail 6.3"
    if (equals4 != true)    return "Fail 6.4"

    if (toString1 != "true")    return "Fail 7.1"
    if (toString2 != "false")   return "Fail 7.2"
    return "OK"
}