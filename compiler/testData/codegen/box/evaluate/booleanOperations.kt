fun <T> T.id() = this

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

const val infixAnd1 = trueVal and trueVal
const val infixAnd2 = trueVal and falseVal
const val infixAnd3 = falseVal and trueVal
const val infixAnd4 = falseVal and falseVal

const val infixOr1 = trueVal or trueVal
const val infixOr2 = trueVal or falseVal
const val infixOr3 = falseVal or trueVal
const val infixOr4 = falseVal or falseVal

const val infixXor1 = trueVal xor trueVal
const val infixXor2 = trueVal xor falseVal
const val infixXor3 = falseVal xor trueVal
const val infixXor4 = falseVal xor falseVal

const val compareTo1 = trueVal.compareTo(trueVal)
const val compareTo2 = trueVal.compareTo(falseVal)
const val compareTo3 = falseVal.compareTo(trueVal)
const val compareTo4 = falseVal.compareTo(falseVal)

const val equals1 = trueVal == trueVal
const val equals2 = trueVal == falseVal
const val equals3 = falseVal == trueVal
const val equals4 = falseVal == falseVal

const val notEquals1 = trueVal != trueVal
const val notEquals2 = trueVal != falseVal
const val notEquals3 = falseVal != trueVal
const val notEquals4 = falseVal != falseVal

const val toString1 = trueVal.toString()
const val toString2 = falseVal.toString()

fun box(): String {
    if (not1.id() != false)  return "Fail 1.1"
    if (not2.id() != true)   return "Fail 1.2"

    if (and1.id() != true)   return "Fail 2.1"
    if (and2.id() != false)  return "Fail 2.2"
    if (and3.id() != false)  return "Fail 2.3"
    if (and4.id() != false)  return "Fail 2.4"

    if (or1.id() != true)    return "Fail 3.1"
    if (or2.id() != true)    return "Fail 3.2"
    if (or3.id() != true)    return "Fail 3.3"
    if (or4.id() != false)   return "Fail 3.4"

    if (xor1.id() != false)  return "Fail 4.1"
    if (xor2.id() != true)   return "Fail 4.2"
    if (xor3.id() != true)   return "Fail 4.3"
    if (xor4.id() != false)  return "Fail 4.4"

    if (infixAnd1.id() != true)   return "Fail 4b.1"
    if (infixAnd2.id() != false)  return "Fail 4b.2"
    if (infixAnd3.id() != false)  return "Fail 4b.3"
    if (infixAnd4.id() != false)  return "Fail 4b.4"

    if (infixOr1.id() != true)    return "Fail 4c.1"
    if (infixOr2.id() != true)    return "Fail 4c.2"
    if (infixOr3.id() != true)    return "Fail 4c.3"
    if (infixOr4.id() != false)   return "Fail 4c.4"

    if (infixXor1.id() != false)  return "Fail 4d.1"
    if (infixXor2.id() != true)   return "Fail 4d.2"
    if (infixXor3.id() != true)   return "Fail 4d.3"
    if (infixXor4.id() != false)  return "Fail 4d.4"

    if (compareTo1.id() != 0)    return "Fail 5.1"
    if (compareTo2.id() != 1)    return "Fail 5.2"
    if (compareTo3.id() != -1)   return "Fail 5.3"
    if (compareTo4.id() != 0)    return "Fail 5.4"

    if (equals1.id() != true)    return "Fail 6.1"
    if (equals2.id() != false)   return "Fail 6.2"
    if (equals3.id() != false)   return "Fail 6.3"
    if (equals4.id() != true)    return "Fail 6.4"

    if (notEquals1.id() != false)   return "Fail 6.5"
    if (notEquals2.id() != true)    return "Fail 6.6"
    if (notEquals3.id() != true)    return "Fail 6.7"
    if (notEquals4.id() != false)   return "Fail 6.8"

    if (toString1.id() != "true")    return "Fail 7.1"
    if (toString2.id() != "false")   return "Fail 7.2"
    return "OK"
}
