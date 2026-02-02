// TARGET_BACKEND: WASM
// MODULE: main
// LANGUAGE: +TurnTypeCheckWarningsIntoErrors

// FILE: jsType.js

class MyJsClass {
    constructor() {
        this.value = 41;
    }

    // Increment the counter by 1 (or a custom step)
    increment() {
        this.value += 1;
        return this.value;
    }
}


// FILE: jsReferenceCasts.kt

class C(val x: Int)

val c = C(1)

fun testTypeOperations(obj: JsReference<C>) : String? {
    if (obj !is Any)
        return "!is Any"
    if (obj !is C)
        return "!is C"
    if (obj != c)
        return "!= C"
    if (obj !== c)
        return "!== C"
    return null
}

// similar checks as above, but with another formal type of the object
fun testTypeOperations(obj: JsReference<String>) : String? {
    if (obj !is Any)
        return "!is Any"
    if (obj is String)
        return "is String"
    // these checks are forbidden by FE checks, although actual runtime code would return 'true' due
    //  to previous unsafe casts
//    if (obj != c)
//        return "!= C"
//    if (obj !is C)
//        return "!is C"
//    if (obj !== c)
//        return "!== C"
    return null
}

fun testTypeOperations(obj: JsAny) : String? {
    if (obj !is Any)
        return "!is Any"
    if (obj !is C)
        return "!is C"
    if (obj is String)
        return "is String"
    if (obj != c)
        return "!= C"
    if (obj !== c)
        return "!== C"
    return null
}

external class MyJsClass() {
    fun increment(): Int
}

fun createExtTypeInstance(): MyJsClass = js("new MyJsClass()")

fun testJsReferenceToExtType(): String? {
    val extInstance: MyJsClass = createExtTypeInstance()    // externref
    val extInstanceAsAny: Any = extInstance                 // JsExternalBox(extInstance)
    val jsReferenceToExtInstanceAsAny: JsReference<Any> = extInstanceAsAny.toJsReference() // JsExternalBox(extInstance) as externref
    val a: Any = jsReferenceToExtInstanceAsAny // JsExternalBox(extInstance)
    val b = a as MyJsClass // unwraps && checks external type
    if (extInstance !== b) return "Fail: extInstance !== b"
    if (a !== b) return "Fail: a !== b"

    val result = a.increment() // smart casted
    if (result != 42) return "Fail: $result != 42"
    return null
}

fun box(): String {
    val jsReference: JsReference<C> = c.toJsReference()
    testTypeOperations(jsReference)?.let { return "Fail: JsReference<C> $it"}

    val jsReferenceWithIncorrectType: JsReference<String> = jsReference.unsafeCast<JsReference<String>>()
    testTypeOperations(jsReferenceWithIncorrectType)?.let { return "Fail: JsReference<C>.unsafeCast<JsReference<String>>() $it"}

    val jsReferenceAsJsAny: JsAny = jsReference
    testTypeOperations(jsReferenceAsJsAny)?.let { return "Fail: (JsReference<C> as JsAny) $it"}

    val c2 : C = jsReference as Any as C
    if (c !== c2)
        return "Fail: implicit cast of JsReference<C>->Any->C shall result in the original object"

    val c3 : C = jsReference as C
    if (c !== c3)
        return "Fail: implicit restoration of JsReference<C>->C shall result in the original object"

    testJsReferenceToExtType()?.let { return it }

    return "OK"
}