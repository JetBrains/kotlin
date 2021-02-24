// FILE: customLibClassName.kt
package customLibClassName

fun main(args: Array<String>) {
    customLib.oneFunSameClassName.oneFunSameFileNameFun()
    customLib.twoFunDifferentSignature.twoFunDifferentSignatureFun()
    customLib.property.foo
    customLib.breakpointOnLocalProperty.breakpointOnLocalPropertyFun()
    customLib.simpleLibFile.foo()
}

// ADDITIONAL_BREAKPOINT: a1.kt / public fun oneFunSameFileNameFun(): Int {
// EXPRESSION: 1 + 1
// RESULT: 2: I

// ADDITIONAL_BREAKPOINT: a1.kt / public fun twoFunDifferentSignatureFun(): Int {
// EXPRESSION: 1 + 2
// RESULT: 3: I

// ADDITIONAL_BREAKPOINT: a1.kt / public val foo: Int =
// EXPRESSION: 1 + 3
// RESULT: 4: I

// ADDITIONAL_BREAKPOINT: a1.kt / public fun breakpointOnLocalPropertyFun(): Int {
// EXPRESSION: 1 + 4
// RESULT: 5: I

// ADDITIONAL_BREAKPOINT: simpleLibFile.kt / public fun foo() {
// EXPRESSION: 1 + 5
// RESULT: 6: I

// FILE: lib/oneFunSameClassName/1/a1.kt
@file:JvmName("SameNameOneFunSameFileName")
@file:JvmMultifileClass
package customLib.oneFunSameClassName

public fun oneFunSameFileNameFun(): Int {
    return 1
}

// FILE: lib/oneFunSameClassName/2/a2.kt
@file:JvmName("SameNameOneFunSameFileName")
@file:JvmMultifileClass
package customLib.oneFunSameClassName

public fun oneFunSameFileNameFun2(): Int {
    return 1
}

// FILE: lib/twoFunDifferentSignature/1/a1.kt
@file:JvmName("SameNameTwoFunDifferentSignature")
@file:JvmMultifileClass
package customLib.twoFunDifferentSignature

public fun twoFunDifferentSignatureFun(): Int {
    return 1
}

// FILE: lib/twoFunDifferentSignature/2/a2.kt
@file:JvmName("SameNameTwoFunDifferentSignature")
@file:JvmMultifileClass
package customLib.twoFunDifferentSignature

public fun twoFunDifferentSignatureFun(i: Int): Int {
    return 1
}

// FILE: lib/breakpointOnLocalProperty/1/a1.kt
@file:JvmName("SameNameBreakpointOnLocalProperty")
@file:JvmMultifileClass
package customLib.breakpointOnLocalProperty

public fun breakpointOnLocalPropertyFun(): Int {
    val a = 1
    return 1
}

// FILE: lib/breakpointOnLocalProperty/2/a2.kt
@file:JvmName("SameNameBreakpointOnLocalProperty")
@file:JvmMultifileClass
package customLib.breakpointOnLocalProperty

public fun breakpointOnLocalPropertyFun2(): Int {
    return 1
}

// FILE: lib/property/1/a1.kt
@file:JvmName("SameNameProperty")
@file:JvmMultifileClass
package customLib.property

public val foo: Int =
    1

// FILE: lib/property/2/a2.kt
@file:JvmName("SameNameProperty")
@file:JvmMultifileClass
package customLib.property

public fun someFun(): Int {
    return 1
}

// FILE: lib/simpleLibFile/simpleLibFile.kt
package customLib.simpleLibFile

public fun foo() {
    1 + 1
}

class B {
    public var prop: Int = 1
}