// WITH_STDLIB
fun <T> T.id() = this

const val someStr = <!EVALUATED("123")!>"123"<!>
const val otherStr = <!EVALUATED("other")!>"other"<!>

const val oneVal = <!EVALUATED("1")!>1<!>
const val oneUnsignedVal = <!EVALUATED("1")!>1u<!>

const val plus1 = <!EVALUATED{IR}("123other")!>someStr.<!EVALUATED{FIR}("123other")!>plus(otherStr)<!><!>
const val plus2 = <!EVALUATED{IR}("1231")!>someStr.<!EVALUATED{FIR}("1231")!>plus(oneVal)<!><!>
const val plus3 = <!EVALUATED{IR}("1231")!>someStr.<!EVALUATED{FIR}("1231")!>plus(oneUnsignedVal)<!><!>

const val length1 = <!EVALUATED{IR}("3")!>someStr.<!EVALUATED{FIR}("3")!>length<!><!>
const val length2 = <!EVALUATED{IR}("5")!>otherStr.<!EVALUATED{FIR}("5")!>length<!><!>

const val get1 = <!EVALUATED{IR}("1")!>someStr.<!EVALUATED{FIR}("1")!>get(0)<!><!>
const val get2 = <!EVALUATED{IR}("t")!>otherStr.<!EVALUATED{FIR}("t")!>get(oneVal)<!><!>

const val compareTo1 = <!EVALUATED{IR}("0")!>someStr.<!EVALUATED{FIR}("0")!>compareTo("123")<!><!>
const val compareTo2 = <!EVALUATED{IR}("-62")!>someStr.<!EVALUATED{FIR}("-62")!>compareTo(otherStr)<!><!>
const val compareTo3 = <!EVALUATED{IR}("62")!>otherStr.<!EVALUATED{FIR}("62")!>compareTo(someStr)<!><!>

const val equals1 = <!EVALUATED("true")!>someStr == "123"<!>
const val equals2 = <!EVALUATED("false")!>someStr == otherStr<!>
const val equals3 = <!EVALUATED("false")!>otherStr == someStr<!>

const val toString1 = <!EVALUATED{IR}("123")!>someStr.<!EVALUATED{FIR}("123")!>toString()<!><!>

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (plus1.id() != "123other")    return "Fail 1.1"
    if (plus2.id() != "1231")        return "Fail 1.2"
    if (plus3.id() != "1231")        return "Fail 1.3"

    if (length1.id() != 3)   return "Fail 2.1"
    if (length2.id() != 5)   return "Fail 2.2"

    if (get1.id() != '1')    return "Fail 3.1"
    if (get2.id() != 't')    return "Fail 3.2"

    if (compareTo1.id() != 0)   return "Fail 4.1"
    if (compareTo2 >= 0)        return "Fail 4.2"
    if (compareTo3 <= 0)        return "Fail 4.3"

    if (equals1.id() != true)    return "Fail 5.1"
    if (equals2.id() != false)   return "Fail 5.2"
    if (equals3.id() != false)   return "Fail 5.3"

    if (toString1.id() != "123") return "Fail 6.1"
    return "OK"
}
