// IGNORE_BACKEND_K2: WASM
// WITH_STDLIB
// FILE: externals.js

const primitives1 = [3.14, "Test string 1",  true, Symbol("symbol"), 131283889534859707199254740992n];
const primitives2 = [3.15, "Test string 2", false, Symbol("symbol"), 231283889534859707199254740992n];

function getPrimitive1(n) {
    return primitives1[n];
}

function getPrimitive2(n) {
    return primitives2[n];
}

function testPrimitive(obj, n) {
    return obj === primitives1[n];
}

function getNewObject() {
    return { value: 123 };
}

function testObject(obj) {
    return obj.value === 123;
}

function getNewArray() {
    return [1, 2, 3];
}

function testArray(obj) {
    return Array.isArray(obj) && obj[0] === 1 && obj[1] === 2 && obj [2] === 3;
}

function roundTrip(x) { return x; }

// FILE: externals.kt
external interface Obj
external interface SubObj : Obj

external fun getPrimitive1(n: Int): Obj
external fun getPrimitive2(n: Int): Obj
external fun testPrimitive(obj: Obj, n: Int): Boolean

external fun getNewObject(): Obj
external fun testObject(obj: Obj): Boolean

external fun getNewArray(): Obj
external fun testArray(obj: Obj): Boolean

external fun roundTrip(x: Obj): Obj

fun assertTrue(x: Boolean) {
    if (!x) error("assertTrue fail")
}

fun assertFalse(x: Boolean) {
    if (x) error("assertFalse fail")
}

fun multiCast(obj: Obj): Obj =
    obj
            as Any as Any
            as Any as? Any
            as Any as Any?
            as Any as? Any?
            as Any as Obj
            as Any as? Obj
            as Any as Obj?
            as Any as? Obj?

            as Any? as Any
            as Any? as? Any
            as Any? as Any?
            as Any? as? Any?
            as Any? as Obj
            as Any? as? Obj
            as Any? as Obj?
            as Any? as? Obj?

            as Obj as Any
            as Obj as? Any
            as Obj as Any?
            as Obj as? Any?
            as Obj as Obj
            as Obj as? Obj
            as Obj as Obj?
            as Obj as? Obj?

            as Obj? as Any
            as Obj? as? Any
            as Obj? as Any?
            as Obj? as? Any?
            as Obj? as Obj
            as Obj? as? Obj
            as Obj? as Obj?
            as Obj? as? Obj?
            as Obj

class CustomClass
interface CustomInterface

fun <T1 : Any, T2 : Any?, T3 : Obj> testWithTypeParameters(obj: Obj, checkObj: (Any?) -> Unit) {
    checkObj(obj as T1)
    checkObj(obj as T2)
    checkObj(obj as T3)
    checkObj(obj as? T1)
    checkObj(obj as? T2)
    checkObj(obj as? T3)
    checkObj(obj as T1?)
    checkObj(obj as T2?)
    checkObj(obj as T3?)
    checkObj(obj as? T1?)
    checkObj(obj as? T2?)
    checkObj(obj as? T3?)
}

fun test(
    obj: Obj,
    anotherObj: Obj,
    checkObj: (Any?) -> Unit,
    stableIdentity: Boolean
) {
    checkObj(obj)
    checkObj(obj as Any)
    checkObj(obj as? Any)
    checkObj(obj as Any?)
    checkObj(obj as? Any?)
    testWithTypeParameters<Any, Any?, Obj>(obj, checkObj)
    val subObj: SubObj = obj as SubObj
    checkObj(subObj)
    checkObj(subObj as Obj)
    val roundTrippedObj = roundTrip(obj)
    checkObj(roundTrippedObj)
    assertTrue(obj == roundTrippedObj)
    if (stableIdentity)
        assertTrue(obj === roundTrippedObj)

    val castedObj = multiCast(roundTrippedObj)
    checkObj(castedObj)

    assertTrue(obj is Any)
    assertTrue(obj is Any?)
    assertFalse(obj !is Any)
    assertFalse(obj !is Any?)

    assertFalse(obj as Any is CustomClass)
    assertFalse(obj as Any is CustomInterface)

    val objects = listOf<Obj>(obj, roundTrippedObj, castedObj)
    for (obj1 in objects) {
        checkObj(obj1)
        assertTrue(obj1.hashCode() == obj.hashCode())
        assertTrue(obj1.toString() == obj.toString())
        assertTrue(obj != anotherObj)
        assertTrue(obj !== anotherObj)
        for (obj2 in objects) {
            assertTrue(obj1 == obj2)
            if (stableIdentity)
                assertTrue(obj1 === obj2)
            assertTrue(obj1.hashCode() == obj2.hashCode())
            assertTrue(obj1.toString() == obj2.toString())
        }
    }
    assertTrue(objects.size == 3)
    assertTrue(objects.toSet().size == 1)
    assertTrue((objects + anotherObj).toSet().size == 2)
}

fun testPrimitive(n: Int) {
    test(
        getPrimitive1(n),
        getPrimitive2(n),
        { obj ->
            if (!testPrimitive(obj as Obj, n)) {
                error("Fail $n")
            }
        },
        stableIdentity = false,
    )
}

class C(val x: Int)
value class IC(val x: Int)

fun box(): String {
    for (i in 0..2) { // TODO: Symbols and BigInts are not supprted in Wasm
        testPrimitive(i)
    }

    test(
        getNewObject(),
        getNewObject(),
        { obj ->
            if (!testObject(obj as Obj)) {
                error("Fail object")
            }
        },
        stableIdentity = true,
    )

    test(
        getNewArray(),
        getNewArray(),
        { obj ->
            if (!testArray(obj as Obj)) {
                error("Fail object")
            }
        },
        stableIdentity = true,
    )


    val kotlinValues = listOf(10, "10", true,  arrayOf(10), intArrayOf(20), C(10), IC(10))
    val otherValues  = listOf(11, "11", false, arrayOf(12), intArrayOf(22), C(11), IC(11))

    for ((value, otherValue) in kotlinValues.zip(otherValues)) {
        test(
            value as Obj,
            otherValues as Obj,
            { obj ->
                if (obj !== value) {
                    error("Fail custom class")
                }
            },
            stableIdentity = true,
        )
    }

    return "OK"
}