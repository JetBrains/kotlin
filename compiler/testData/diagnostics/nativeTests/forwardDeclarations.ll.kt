// LL_FIR_DIVERGENCE
// KT-62936
// LL_FIR_DIVERGENCE
// WITH_STDLIB

// FILE: stdlib.kt

package kotlinx.cinterop

abstract class CStructVar
interface ObjCObject
abstract class ObjCObjectBase : ObjCObject

// FILE: cnames.kt

package cnames.structs

class FwdStruct {}

// FILE: objcnamesClasses.kt

package objcnames.classes

class FwdObjcClass {}

// FILE: objcnamesProtocols.kt

package objcnames.protocols

interface FwdProtocol {}

// FILE: lib.kt

package lib

import kotlinx.cinterop.*

class FwdStruct : CStructVar()
class FwdObjcClass : ObjCObjectBase()
interface FwdProtocol : ObjCObject

// FILE: lib2.kt

package lib2

class FwdStruct
class FwdObjcClass
interface FwdProtocol


// FILE: main.kt

// this inspections differs in K1/K2, but unrelated to what is tested in this test
@file:Suppress("UNUSED_PARAMETER", "UNUSED_EXPRESSION", "UNUSED_VARIABLE", <!ERROR_SUPPRESSION!>"INCOMPATIBLE_TYPES"<!>)

fun testUnckeckedAsFromAny(x: Any) {
    x as? cnames.structs.FwdStruct
    x as? objcnames.classes.FwdObjcClass
    x as? objcnames.protocols.FwdProtocol
    if (1 > 0) { x as cnames.structs.FwdStruct }
    if (1 > 0) { x as objcnames.classes.FwdObjcClass }
    if (1 > 0) { x as objcnames.protocols.FwdProtocol }
}

fun testIs(x: Any) : Int {
    return when {
        x is cnames.structs.FwdStruct -> 1
        x is objcnames.classes.FwdObjcClass -> 2
        x is objcnames.protocols.FwdProtocol -> 3
        else -> 4
    }
}

fun testIs2(x: lib.FwdStruct) = x is cnames.structs.FwdStruct
fun testIs3(x: lib.FwdObjcClass) = x is objcnames.classes.FwdObjcClass
fun testIs4(x: lib.FwdProtocol) = x is objcnames.protocols.FwdProtocol


fun testClass1(x : cnames.structs.FwdStruct) = x::class
fun testClass2(x : objcnames.classes.FwdObjcClass) = x::class
fun testClass3(x : objcnames.protocols.FwdProtocol) = x::class
fun testClass4() {
    cnames.structs.FwdStruct::class
    objcnames.classes.FwdObjcClass::class
    objcnames.protocols.FwdProtocol::class
}
inline fun <reified T> inlineF(x: T) {}

fun testInline1(x : cnames.structs.FwdStruct) = inlineF(x)
fun testInline2(x : objcnames.classes.FwdObjcClass) = inlineF(x)
fun testInline3(x : objcnames.protocols.FwdProtocol) = inlineF(x)
fun testInline4() {
    val a : (cnames.structs.FwdStruct) -> Unit = ::inlineF
    val b : (objcnames.classes.FwdObjcClass) -> Unit = ::inlineF
    val c : (objcnames.protocols.FwdProtocol) -> Unit = ::inlineF
}

fun testCheckedAs1(x : lib.FwdStruct) = x as cnames.structs.FwdStruct
fun testCheckedAs2(x : lib.FwdObjcClass) = x as objcnames.classes.FwdObjcClass
fun testCheckedAs3(x : lib.FwdProtocol) = x as objcnames.protocols.FwdProtocol
fun testCheckedSafeAs4(x : lib.FwdStruct) = x as? cnames.structs.FwdStruct
fun testCheckedSafeAs5(x : lib.FwdObjcClass) = x as? objcnames.classes.FwdObjcClass
fun testCheckedSafeAs6(x : lib.FwdProtocol) = x as? objcnames.protocols.FwdProtocol

fun testUnCheckedAs1(x : lib2.FwdStruct) = x as cnames.structs.FwdStruct
fun testUnCheckedAs2(x : lib2.FwdObjcClass) = x as objcnames.classes.FwdObjcClass
fun testUnCheckedAs3(x : lib2.FwdProtocol) = x as objcnames.protocols.FwdProtocol

fun testUnCheckedAs4(x : lib.FwdStruct) = x as objcnames.classes.FwdObjcClass
