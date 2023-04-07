// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
// TARGET_BACKEND: JS_IR

const val someStr = <!EVALUATED("123")!>"123"<!>
const val otherStr = <!EVALUATED("other")!>"other"<!>

const val oneVal = <!EVALUATED("1")!>1<!>

const val plus1 = someStr.<!EVALUATED("123other")!>plus(otherStr)<!>
const val plus2 = someStr.<!EVALUATED("1231")!>plus(oneVal)<!>

const val length1 = someStr.<!EVALUATED("3")!>length<!>
const val length2 = otherStr.<!EVALUATED("5")!>length<!>

const val get1 = someStr.<!EVALUATED("1")!>get(0)<!>
const val get2 = otherStr.<!EVALUATED("t")!>get(oneVal)<!>

const val compareTo1 = someStr.<!EVALUATED("0")!>compareTo("123")<!>
const val compareTo2 = someStr.<!EVALUATED("-62")!>compareTo(otherStr)<!>
const val compareTo3 = otherStr.<!EVALUATED("62")!>compareTo(someStr)<!>

const val equals1 = <!EVALUATED("true")!>someStr == "123"<!>
const val equals2 = <!EVALUATED("false")!>someStr == otherStr<!>
const val equals3 = <!EVALUATED("false")!>otherStr == someStr<!>

const val toString1 = someStr.<!EVALUATED("123")!>toString()<!>

fun box(): String {
    if (<!EVALUATED("false")!>plus1 != "123other"<!>)    return "Fail 1.1"
    if (<!EVALUATED("false")!>plus2 != "1231"<!>)        return "Fail 1.2"

    if (<!EVALUATED("false")!>length1 != 3<!>)   return "Fail 2.1"
    if (<!EVALUATED("false")!>length2 != 5<!>)   return "Fail 2.2"

    if (<!EVALUATED("false")!>get1 != '1'<!>)    return "Fail 3.1"
    if (<!EVALUATED("false")!>get2 != 't'<!>)    return "Fail 3.2"

    if (<!EVALUATED("false")!>compareTo1 != 0<!>)    return "Fail 4.1"
    if (<!EVALUATED("false")!>compareTo2 >= 0<!>)    return "Fail 4.2"
    if (<!EVALUATED("false")!>compareTo3 <= 0<!>)    return "Fail 4.3"

    if (<!EVALUATED("false")!>equals1 != true<!>)    return "Fail 5.1"
    if (<!EVALUATED("false")!>equals2 != false<!>)   return "Fail 5.2"
    if (<!EVALUATED("false")!>equals3 != false<!>)   return "Fail 5.3"

    if (<!EVALUATED("false")!>toString1 != "123"<!>) return "Fail 6.1"
    return "OK"
}
