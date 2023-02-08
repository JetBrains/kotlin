// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
// TARGET_BACKEND: JS_IR

const val someStr = "123"
const val otherStr = "other"

const val oneVal = 1

const val plus1 = someStr.plus(otherStr)
const val plus2 = someStr.plus(oneVal)

const val length1 = someStr.length
const val length2 = otherStr.length

const val get1 = someStr.get(0)
const val get2 = otherStr.get(oneVal)

const val compareTo1 = someStr.compareTo("123")
const val compareTo2 = someStr.compareTo(otherStr)
const val compareTo3 = otherStr.compareTo(someStr)

const val equals1 = someStr == "123"
const val equals2 = someStr == otherStr
const val equals3 = otherStr == someStr

const val toString1 = someStr.toString()

fun box(): String {
    if (plus1 != "123other")    return "Fail 1.1"
    if (plus2 != "1231")        return "Fail 1.2"

    if (length1 != 3)   return "Fail 2.1"
    if (length2 != 5)   return "Fail 2.2"

    if (get1 != '1')    return "Fail 3.1"
    if (get2 != 't')    return "Fail 3.2"

    if (compareTo1 != 0)    return "Fail 4.1"
    if (compareTo2 >= 0)    return "Fail 4.2"
    if (compareTo3 <= 0)    return "Fail 4.3"

    if (equals1 != true)    return "Fail 5.1"
    if (equals2 != false)   return "Fail 5.2"
    if (equals3 != false)   return "Fail 5.3"

    if (toString1 != "123") return "Fail 6.1"
    return "OK"
}