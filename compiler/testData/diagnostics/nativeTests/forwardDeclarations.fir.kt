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
    <!UNCHECKED_CAST_TO_FORWARD_DECLARATION!>x as? cnames.structs.FwdStruct<!>
    <!UNCHECKED_CAST_TO_FORWARD_DECLARATION!>x as? objcnames.classes.FwdObjcClass<!>
    <!UNCHECKED_CAST_TO_FORWARD_DECLARATION!>x as? objcnames.protocols.FwdProtocol<!>
    if (1 > 0) { <!UNCHECKED_CAST_TO_FORWARD_DECLARATION!>x as cnames.structs.FwdStruct<!> }
    if (1 > 0) { <!UNCHECKED_CAST_TO_FORWARD_DECLARATION!>x as objcnames.classes.FwdObjcClass<!> }
    if (1 > 0) { <!UNCHECKED_CAST_TO_FORWARD_DECLARATION!>x as objcnames.protocols.FwdProtocol<!> }
}

fun testIs(x: Any) : Int {
    return when {
        <!CANNOT_CHECK_FOR_FORWARD_DECLARATION!>x is cnames.structs.FwdStruct<!> -> 1
        <!CANNOT_CHECK_FOR_FORWARD_DECLARATION!>x is objcnames.classes.FwdObjcClass<!> -> 2
        <!CANNOT_CHECK_FOR_FORWARD_DECLARATION!>x is objcnames.protocols.FwdProtocol<!> -> 3
        else -> 4
    }
}

fun testIs2(x: lib.FwdStruct) = <!CANNOT_CHECK_FOR_FORWARD_DECLARATION!>x is cnames.structs.FwdStruct<!>
fun testIs3(x: lib.FwdObjcClass) = <!CANNOT_CHECK_FOR_FORWARD_DECLARATION!>x is objcnames.classes.FwdObjcClass<!>
fun testIs4(x: lib.FwdProtocol) = <!CANNOT_CHECK_FOR_FORWARD_DECLARATION!>x is objcnames.protocols.FwdProtocol<!>


fun testClass1(x : cnames.structs.FwdStruct) = x::class
fun testClass2(x : objcnames.classes.FwdObjcClass) = x::class
fun testClass3(x : objcnames.protocols.FwdProtocol) = x::class
fun testClass4() {
    <!FORWARD_DECLARATION_AS_CLASS_LITERAL!>cnames.structs.FwdStruct::class<!>
    <!FORWARD_DECLARATION_AS_CLASS_LITERAL!>objcnames.classes.FwdObjcClass::class<!>
    <!FORWARD_DECLARATION_AS_CLASS_LITERAL!>objcnames.protocols.FwdProtocol::class<!>
}
inline fun <reified T> inlineF(x: T) {}

fun testInline1(x : cnames.structs.FwdStruct) = <!FORWARD_DECLARATION_AS_REIFIED_TYPE_ARGUMENT!>inlineF<!>(x)
fun testInline2(x : objcnames.classes.FwdObjcClass) = <!FORWARD_DECLARATION_AS_REIFIED_TYPE_ARGUMENT!>inlineF<!>(x)
fun testInline3(x : objcnames.protocols.FwdProtocol) = <!FORWARD_DECLARATION_AS_REIFIED_TYPE_ARGUMENT!>inlineF<!>(x)
fun testInline4() {
    val a : (cnames.structs.FwdStruct) -> Unit = ::<!FORWARD_DECLARATION_AS_REIFIED_TYPE_ARGUMENT!>inlineF<!>
    val b : (objcnames.classes.FwdObjcClass) -> Unit = ::<!FORWARD_DECLARATION_AS_REIFIED_TYPE_ARGUMENT!>inlineF<!>
    val c : (objcnames.protocols.FwdProtocol) -> Unit = ::<!FORWARD_DECLARATION_AS_REIFIED_TYPE_ARGUMENT!>inlineF<!>
}

fun testCheckedAs1(x : lib.FwdStruct) = x as cnames.structs.FwdStruct
fun testCheckedAs2(x : lib.FwdObjcClass) = x as objcnames.classes.FwdObjcClass
fun testCheckedAs3(x : lib.FwdProtocol) = x as objcnames.protocols.FwdProtocol
fun testCheckedSafeAs4(x : lib.FwdStruct) = x as? cnames.structs.FwdStruct
fun testCheckedSafeAs5(x : lib.FwdObjcClass) = x as? objcnames.classes.FwdObjcClass
fun testCheckedSafeAs6(x : lib.FwdProtocol) = x as? objcnames.protocols.FwdProtocol

fun testUnCheckedAs1(x : lib2.FwdStruct) = <!UNCHECKED_CAST_TO_FORWARD_DECLARATION!>x as cnames.structs.FwdStruct<!>
fun testUnCheckedAs2(x : lib2.FwdObjcClass) = <!UNCHECKED_CAST_TO_FORWARD_DECLARATION!>x as objcnames.classes.FwdObjcClass<!>
fun testUnCheckedAs3(x : lib2.FwdProtocol) = <!UNCHECKED_CAST_TO_FORWARD_DECLARATION!>x as objcnames.protocols.FwdProtocol<!>

fun testUnCheckedAs4(x : lib.FwdStruct) = <!UNCHECKED_CAST_TO_FORWARD_DECLARATION!>x as objcnames.classes.FwdObjcClass<!>
