// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE
// !LANGUAGE: +ContractEffects

import kotlin.internal.*

@Returns(ConstantValue.TRUE)
@JoinConditions(JoiningStrategy.ALL)
fun allIsString(
        @IsInstance(String::class) x: Any?,
        @IsInstance(String::class) y: Any?,
        @IsInstance(String::class) z: Any?
) = x is String && y is String && z is String

fun testAllJoiner(x: Any?, y: Any?, z: Any?) {
    if (allIsString(x, y, z)) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
        <!DEBUG_INFO_SMARTCAST!>y<!>.length
        <!DEBUG_INFO_SMARTCAST!>z<!>.length
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        y.<!UNRESOLVED_REFERENCE!>length<!>
        z.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

@Returns(ConstantValue.TRUE)
@JoinConditions(JoiningStrategy.NONE)
fun noneIsNotString(
        @Not @IsInstance(String::class) x: Any?,
        @Not @IsInstance(String::class) y: Any?,
        @Not @IsInstance(String::class) z: Any?
) = (!(x !is String)) && (!(y !is String)) && (!(z !is String))

fun testNoneJoiner(x: Any?, y: Any?, z: Any?) {
    if (noneIsNotString(x, y, z)) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
        <!DEBUG_INFO_SMARTCAST!>y<!>.length
        <!DEBUG_INFO_SMARTCAST!>z<!>.length
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        y.<!UNRESOLVED_REFERENCE!>length<!>
        z.<!UNRESOLVED_REFERENCE!>length<!>
    }
}


@Returns(ConstantValue.TRUE)
@JoinConditions(JoiningStrategy.ANY) // pretty pointless, just for testing purposes
fun anyIsString(@IsInstance(String::class) x: Any?) = x !is String

fun testNoneJoiner(x: Any?) {
    if (anyIsString(x)) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}