// TARGET_BACKEND: WASM
// USE_SHARED_OBJECTS
// WASM_FAILS_IN: SM, JSC
// MODULE: main


// FILE: shared.kt

fun box(): String {
    val res = testJsReferences()
    if (res != "OK") return res

    return testVirtualCalls()
}

abstract class MyClassBase {
    abstract fun foo(): Int
}

class MyClass(val a: Int, val b: Int) : Function2<Int, Int, Int>, MyClassBase() {
    override fun invoke(p1: Int, p2: Int): Int = p1 - a + p2 * b
    override fun foo() = 5
}

fun testVirtualCalls() : String {
    val myObj : MyClass = MyClass(1, 2)
    val myObjAsBase: MyClassBase = myObj
    val myObjAsFunc: Function2<Int, Int, Int> = myObj

    if (myObj.a != 1) return "Fail: get field"
    if (myObjAsFunc.invoke(3, 4) != 10) return "Fail: interface call"
    if (myObjAsBase.foo() != 5) return "Fail: virtual method call"

    return "OK"
}

class C(val x: Int)

var unsharedRef : JsAny? = null
var shareableRef : JsShareableAny? = null
var kotlinRef : Any? = null
var someInt : Int = 0

fun testJsReferences(): String {
    val c = C(1)
    val jsReference: JsReference<C> = c.toJsReference()
    if (jsReference is C)
        return "Fail: JsReference is not Kotlin type"
//    if (c !== (ref as C)) // currently does not work due to WasmBaseTypeOperatorTransformer.generateIsSubClass()
//        return "Fail: conversion JsReference -> KotlinType"

    // test JsReference -> Any conversion
    kotlinRef = jsReference
    if (kotlinRef !== c)
        return "Fail: Any -> JsReference -> Any conversion"

    // test Any -> JsAny -> JsShareableAny -> Any casts
    unsharedRef = (c as Any) as JsAny
    shareableRef = unsharedRef as JsShareableAny
    if (jsReference !== shareableRef)
        return "Fail: conversion Any -> JsAny -> JsShareableAny"
    if (c !== (shareableRef as Any))
        return "Fail: conversion Any -> JsAny -> JsShareableAny -> Any"
//    if (c !== (someShareableRef as C))  // currently does not work due to WasmBaseTypeOperatorTransformer.generateIsSubClass()
//        return "Fail: conversion JsShareableAny -> KotlinType"

    // test JsReference -> JsAny conversion and back
    unsharedRef = jsReference
    if (jsReference !== (unsharedRef as JsShareableAny))
        return "Fail: conversion JsReference->JsAny->JsShareableAny"

    // test hash codes
    if (c.hashCode() != jsReference.hashCode())
        return "Fail: JsReference.hashCode"
    if (c.hashCode() != shareableRef.hashCode())
        return "Fail: JsShareableAny.hashCode"
    if (c.hashCode() != unsharedRef.hashCode())
        return "Fail: (jsReference as JsAny).hashCode"

    return "OK"
}
