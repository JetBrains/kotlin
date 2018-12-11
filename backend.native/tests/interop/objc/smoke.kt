/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlinx.cinterop.*
import objcSmoke.*
import kotlin.native.ref.*
import kotlin.test.*

fun main(args: Array<String>) {
    autoreleasepool {
        run()
    }
}

fun run() {
    testTypeOps()
    testConversions()
    testWeakRefs()

    assertEquals(2, ForwardDeclaredEnum.TWO.value)

    println(
            getSupplier(
                    invoke1(42) { it * 2 }
            )!!()
    )

    val foo = Foo()

    val classGetter = getClassGetter(foo)
    invoke2 { println(classGetter()) }

    foo.hello()
    foo.name = "everybody"
    foo.helloWithPrinter(object : NSObject(), PrinterProtocol {
        override fun print(string: CPointer<ByteVar>?) {
            println("Kotlin says: " + string?.toKString())
        }
    })

    Bar().hello()

    val pair = MutablePairImpl(42, 17)
    replacePairElements(pair, 1, 2)
    pair.swap()
    println("${pair.first}, ${pair.second}")

    val defaultPair = MutablePairImpl()
    assertEquals(defaultPair.first(), 123)
    assertEquals(defaultPair.second(), 321)

    // equals and hashCode (virtually):
    val map = mapOf(foo to pair, pair to foo)

    // equals (directly):
    if (!foo.equals(pair)) {
        // toString (directly):
        println(map[pair].toString() + map[foo].toString() == foo.description() + pair.description())
    }

    // hashCode (directly):
    if (foo.hashCode() == foo.hash().let { it.toInt() xor (it shr 32).toInt() }) {
        // toString (virtually):
        println(map.keys.map { it.toString() }.min() == foo.description())
    }

    println(globalString)
    autoreleasepool {
        globalString = "Another global string"
    }
    println(globalString)

    println(globalObject)
    globalObject = object : NSObject() {
        override fun description() = "global object"
    }
    println(globalObject)

    println(formatStringLength("%d %d", 42, 17))

    println(STRING_MACRO)
    println(CFSTRING_MACRO)
}

fun MutablePairProtocol.swap() {
    update(0, add = second)
    update(1, sub = first)
    update(0, add = second)
    update(1, sub = second*2)
}

class Bar : Foo() {
    override fun helloWithPrinter(printer: PrinterProtocol?) = memScoped {
        printer!!.print("Hello from Kotlin".cstr.getPointer(memScope))
    }
}

@Suppress("CONFLICTING_OVERLOADS")
class MutablePairImpl(first: Int, second: Int) : NSObject(), MutablePairProtocol {
    private var elements = intArrayOf(first, second)

    override fun first() = elements.first()
    override fun second() = elements.last()

    override fun update(index: Int, add: Int) {
        elements[index] += add
    }

    override fun update(index: Int, sub: Int) {
        elements[index] -= sub
    }

    constructor() : this(123, 321)
}

fun testTypeOps() {
    assertTrue(99.asAny() is NSNumber)
    assertTrue(null.asAny() is NSNumber?)
    assertFalse(null.asAny() is NSNumber)
    assertFalse("".asAny() is NSNumber)
    assertTrue("bar".asAny() is NSString)

    assertTrue(Foo.asAny() is FooMeta)
    assertTrue(Foo.asAny() is NSObjectMeta)
    assertTrue(Foo.asAny() is NSObject)
    assertFalse(Foo.asAny() is Foo)
    assertTrue(NSString.asAny() is NSCopyingProtocolMeta)
    assertFalse(NSString.asAny() is NSCopyingProtocol)
    assertTrue(NSValue.asAny() is NSObjectProtocolMeta)
    assertFalse(NSValue.asAny() is NSObjectProtocol) // Must be true, but not implemented properly yet.

    assertEquals(3u, ("foo" as NSString).length())
    assertEquals(4u, ((1..4).joinToString("") as NSString).length())
    assertEquals(2u, (listOf(0, 1) as NSArray).count())
    assertEquals(42L, (42 as NSNumber).longLongValue())

    assertFails { "bar" as NSNumber }
    assertFails { 42 as NSArray }
    assertFails { listOf(1) as NSString }
    assertFails { NSObject() as Bar }
    assertFails { NSObject() as NSValue }

    MutablePairImpl(1, 2).asAny() as MutablePairProtocol
    assertFails { MutablePairImpl(1, 2).asAny() as Foo }
}

fun testConversions() {
    testMethodsOfAny(emptyList<Nothing>(), NSArray())
    testMethodsOfAny(listOf(1, "foo"), nsArrayOf(1, "foo"))
    testMethodsOfAny(42, NSNumber.numberWithInt(42), 17)
}

fun testMethodsOfAny(kotlinObject: Any, equalNsObject: NSObject, otherObject: Any = Any()) {
    assertEquals(kotlinObject.hashCode(), equalNsObject.hashCode())
    assertEquals(kotlinObject.toString(), equalNsObject.toString())
    assertEquals(kotlinObject, equalNsObject)
    assertEquals(equalNsObject, kotlinObject)
    assertNotEquals(equalNsObject, otherObject)
}

fun testWeakRefs() {
    testWeakReference({ NSObject.new()!! })

    createAndAbandonWeakRef(NSObject())

    testWeakReference({ NSArray.arrayWithArray(listOf(42)) as NSArray })
}

fun testWeakReference(block: () -> NSObject) {
    val ref = autoreleasepool {
        createAndTestWeakReference(block)
    }

    assertNull(ref.get())
}

fun createAndTestWeakReference(block: () -> NSObject): WeakReference<NSObject> {
    val ref = createWeakReference(block)
    assertNotNull(ref.get())
    assertEquals(ref.get()!!.hash(), ref.get()!!.hash())
    return ref
}

fun createWeakReference(block: () -> NSObject) = WeakReference(block())

fun createAndAbandonWeakRef(obj: NSObject) {
    WeakReference(obj)
}

fun nsArrayOf(vararg elements: Any): NSArray = NSMutableArray().apply {
    elements.forEach {
        this.addObject(it as ObjCObject)
    }
}

fun Any?.asAny(): Any? = this
