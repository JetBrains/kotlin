// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

@Returns
fun myAssert(@Equals(ConstantValue.TRUE) condition: Boolean) {
    if (!condition) throw kotlin.IllegalArgumentException("Assertion failed")
}

@Returns(ConstantValue.TRUE)
fun isString(@IsInstance(String::class) x: Any?) = x is String

@Returns(ConstantValue.TRUE)
fun isInt(@IsInstance(Int::class) x: Any?) = x is Int

@Returns(ConstantValue.TRUE)
fun notEqualsNull(@Equals(ConstantValue.NOT_NULL) x: Any?) = x != null

@Returns(ConstantValue.TRUE)
fun equalsTrue(@Equals(ConstantValue.TRUE) x: Boolean) = x == true

@Returns(ConstantValue.TRUE)
fun equalsFalse(@Equals(ConstantValue.FALSE) x: Boolean) = x == false

@Returns(ConstantValue.NOT_NULL)
fun nullWhenNotString(@IsInstance(String::class) x: Any?) = x as? String

fun nested1(x: Any?) {
    if (equalsTrue(isString(x))) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun nested2(x: Any?) {
    myAssert(equalsTrue(isString(x)))
    <!DEBUG_INFO_SMARTCAST!>x<!>.length
}

fun nested3(x: Any?) {
    myAssert(equalsTrue(notEqualsNull(nullWhenNotString(x))))
    <!DEBUG_INFO_SMARTCAST!>x<!>.length
}

fun branchedAndNested(x: Any?, y: Any?) {
    myAssert(equalsTrue(notEqualsNull(nullWhenNotString(x))) && equalsTrue(isString(y)))
    <!DEBUG_INFO_SMARTCAST!>x<!>.length
    <!DEBUG_INFO_SMARTCAST!>y<!>.length
}


fun br(y: Any?) {
    if (myAssert(y is Int) == Unit && myAssert(y is String) == Unit) {
        <!DEBUG_INFO_SMARTCAST!>y<!>.length
        <!DEBUG_INFO_SMARTCAST!>y<!>.inc()
    }
}

fun branchedAndNestedWithNativeOperators(x: Any?, y: Any?) {
    myAssert(
            equalsTrue(notEqualsNull(nullWhenNotString(x)))   // x is String
            &&
            (
                    (myAssert(y is Int) == Unit && myAssert(y is String) == Unit)  // y is Int, String
                    ||
                    equalsTrue(isInt(y) && isString(y))                          // y is Int, String
            )
            &&
            (1 == 2 || <!USELESS_IS_CHECK!>y is Int<!> || isString(y))
    )
    <!DEBUG_INFO_SMARTCAST!>x<!>.length
    <!DEBUG_INFO_SMARTCAST!>y<!>.length
    <!DEBUG_INFO_SMARTCAST!>y<!>.inc()
}

