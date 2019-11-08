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
    testConversions()
    testTypeOps()
    testWeakRefs()
    testExceptions()
    testBlocks()
    testCustomRetain()
    testVarargs()
    testOverrideInit()
    testMultipleInheritanceClash()
    testClashingWithAny()
    testInitWithCustomSelector()
    testAllocNoRetain()
    testNSOutputStreamToMemoryConstructor()
    testExportObjCClass()
    testCustomString()
    testLocalizedStrings()

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
    // hash() returns value of NSUInteger type.
    val hash = when (Platform.osFamily) {
        // `typedef unsigned int NSInteger` on watchOS.
        OsFamily.WATCHOS -> foo.hash().toInt()
        // `typedef unsigned long NSUInteger` on iOS, macOS, tvOS.
        else -> foo.hash().let { it.toInt() xor (it shr 32).toInt() }
    }
    if (foo.hashCode() == hash) {
        // toString (virtually):
        if (Platform.memoryModel == MemoryModel.STRICT)
            println(map.keys.map { it.toString() }.min() == foo.description())
        else
            // TODO: hack until proper cycle collection in maps.
            println(true)
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

    // Ensure that overriding method bridge has retain-autorelease sequence:
    createObjectWithFactory(object : NSObject(), ObjectFactoryProtocol {
        override fun create() = autoreleasepool { NSObject() }
    })

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

interface Zzz

fun testTypeOps() {
    assertTrue(99.asAny() is NSNumber)
    assertTrue(null.asAny() is NSNumber?)
    assertFalse(null.asAny() is NSNumber)
    assertFalse("".asAny() is NSNumber)
    assertTrue("bar".asAny() is NSString)

    assertTrue(Foo.asAny() is FooMeta)
    assertFalse(Foo.asAny() is Zzz)
    assertTrue(Foo.asAny() is NSObjectMeta)
    assertTrue(Foo.asAny() is NSObject)
    assertFalse(Foo.asAny() is Foo)
    assertTrue(NSString.asAny() is NSCopyingProtocolMeta)
    assertFalse(NSString.asAny() is NSCopyingProtocol)
    assertTrue(NSValue.asAny() is NSObjectProtocolMeta)
    assertFalse(NSValue.asAny() is NSObjectProtocol) // Must be true, but not implemented properly yet.

    assertFalse(Any() is ObjCClass)
    assertFalse(Any() is ObjCClassOf<*>)
    assertFalse(NSObject().asAny() is ObjCClass)
    assertFalse(NSObject().asAny() is ObjCClassOf<*>)
    assertTrue(NSObject.asAny() is ObjCClass)
    assertTrue(NSObject.asAny() is ObjCClassOf<*>)

    assertFalse(Any() is ObjCProtocol)
    assertTrue(getPrinterProtocolRaw() is ObjCProtocol)
    val printerProtocol = getPrinterProtocol()!!
    assertTrue(printerProtocol.asAny() is ObjCProtocol)

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
    testMethodsOfAny(true, NSNumber.numberWithBool(true), false)
}

fun testMethodsOfAny(kotlinObject: Any, equalNsObject: NSObject, otherObject: Any = Any()) {
    assertEquals(kotlinObject.hashCode(), equalNsObject.hashCode())
    assertEquals(kotlinObject.toString(), equalNsObject.toString())
    assertEquals(kotlinObject, equalNsObject)
    assertEquals(equalNsObject, kotlinObject)
    assertNotEquals(equalNsObject, otherObject)
}

fun testWeakRefs() {
    testWeakReference({ createNSObject()!! })

    createAndAbandonWeakRef(NSObject())

    testWeakReference({ NSArray.arrayWithArray(listOf(42)) as NSArray })
}

fun testWeakReference(block: () -> NSObject) {
    val ref = autoreleasepool {
        createAndTestWeakReference(block)
    }

    kotlin.native.internal.GC.collect()

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

fun testExceptions() {
    assertFailsWith<MyException> {
        ExceptionThrowerManager.throwExceptionWith(object : NSObject(), ExceptionThrowerProtocol {
            override fun throwException() {
                throw MyException()
            }
        })
    }
}

fun testBlocks() {
    assertTrue(Blocks.blockIsNull(null))
    assertFalse(Blocks.blockIsNull({}))

    assertEquals(null, Blocks.nullBlock)
    assertNotEquals(null, Blocks.notNullBlock)

    assertEquals(10, Blocks.same({ a, b, c, d -> a + b + c + d })!!(1, 2, 3, 4))

    assertEquals(222, callProvidedBlock(object : NSObject(), BlockProviderProtocol {
        override fun block(): (Int) -> Int = { it * 2 }
    }, 111))

    assertEquals(322, callPlusOneBlock(object : NSObject(), BlockConsumerProtocol {
        override fun callBlock(block: ((Int) -> Int)?, argument: Int) = block!!(argument)
    }, 321))
}

private lateinit var retainedMustNotBeDeallocated: MustNotBeDeallocated

fun testCustomRetain() {
    fun test() {
        useCustomRetainMethods(object : Foo(), CustomRetainMethodsProtocol {
            override fun returnRetained(obj: Any?) = obj
            override fun consume(obj: Any?) {}
            override fun consumeSelf() {}
            override fun returnRetainedBlock(block: (() -> Unit)?) = block
        })

        CustomRetainMethodsImpl().let {
            it.returnRetained(Any())
            retainedMustNotBeDeallocated = MustNotBeDeallocated() // Retain to detect possible over-release.
            it.consume(retainedMustNotBeDeallocated)
            it.consumeSelf()
            it.returnRetainedBlock({})!!()
        }
    }

    autoreleasepool {
        test()
        kotlin.native.internal.GC.collect()
    }

    assertFalse(unexpectedDeallocation)
}

fun testVarargs() {
    assertEquals(
            "a b -1",
            TestVarargs.testVarargsWithFormat(
                    "%@ %s %d",
                    "a" as NSString, "b".cstr, (-1).toByte()
            ).formatted
    )

    assertEquals(
            "2 3 9223372036854775807",
            TestVarargs(
                    "%d %d %lld",
                    2.toShort(), 3, Long.MAX_VALUE
            ).formatted
    )

    assertEquals(
            "0.1 0.2 1 0",
            TestVarargs.create(
                    "%.1f %.1lf %d %d",
                    0.1.toFloat(), 0.2, true, false
            ).formatted
    )

    assertEquals(
            "1 2 3",
            TestVarargs(
                    format = "%d %d %d",
                    args = *arrayOf(1, 2, 3)
            ).formatted
    )

    assertEquals(
            "4 5 6",
            TestVarargs(
                    args = *arrayOf(4, *arrayOf(5, 6)),
                    format = "%d %d %d"
            ).formatted
    )

    assertEquals(
            "7",
            TestVarargsSubclass.stringWithFormat(
                    "%d",
                    7
            )
    )
}

fun testOverrideInit() {
    assertEquals(42, (TestOverrideInitImpl.createWithValue(42) as TestOverrideInitImpl).value)
}

class TestOverrideInitImpl @OverrideInit constructor(val value: Int) : TestOverrideInit(value) {
    companion object : TestOverrideInitMeta()
}

private class MyException : Throwable()

fun testMultipleInheritanceClash() {
    val clash1 = MultipleInheritanceClash1()
    val clash2 = MultipleInheritanceClash2()

    clash1.delegate = clash1
    assertEquals(clash1, clash1.delegate)
    clash1.setDelegate(clash2)
    assertEquals(clash2, clash1.delegate())

    clash2.delegate = clash1
    assertEquals(clash1, clash2.delegate)
    clash2.setDelegate(clash2)
    assertEquals(clash2, clash2.delegate())
}

fun testClashingWithAny() {
    assertEquals("description", TestClashingWithAny1().toString())
    assertEquals("toString", TestClashingWithAny1().toString_())
    assertEquals("toString_", TestClashingWithAny1().toString__())
    assertEquals(1, TestClashingWithAny1().hashCode())
    assertEquals(31, TestClashingWithAny1().hashCode_())
    assertFalse(TestClashingWithAny1().equals(TestClashingWithAny1()))
    assertTrue(TestClashingWithAny1().equals_(TestClashingWithAny1()))

    assertEquals("description", TestClashingWithAny2().toString())
    assertEquals(Unit, TestClashingWithAny2().toString_())
    assertEquals(2, TestClashingWithAny2().hashCode())
    assertEquals(Unit, TestClashingWithAny2().hashCode_())
    assertFalse(TestClashingWithAny2().equals(TestClashingWithAny2()))
    assertEquals(Unit, TestClashingWithAny2().equals_(42))

    assertEquals("description", TestClashingWithAny3().toString())
    assertEquals("toString:11", TestClashingWithAny3().toString(11))
    assertEquals(3, TestClashingWithAny3().hashCode())
    assertEquals(4, TestClashingWithAny3().hashCode(3))
    assertFalse(TestClashingWithAny3().equals(TestClashingWithAny3()))
    assertTrue(TestClashingWithAny3().equals())
}

fun testInitWithCustomSelector() {
    assertFalse(TestInitWithCustomSelector().custom)
    assertTrue(TestInitWithCustomSelector(custom = Unit).custom)

    val customSubclass: TestInitWithCustomSelector = TestInitWithCustomSelectorSubclass.createCustom()
    assertTrue(customSubclass is TestInitWithCustomSelectorSubclass)
    assertTrue(customSubclass.custom)

    // Test side effect:
    var ok = false
    assertTrue(TestInitWithCustomSelector(run { ok = true }).custom)
    assertTrue(ok)
}

private class TestInitWithCustomSelectorSubclass : TestInitWithCustomSelector {
    @OverrideInit constructor(custom: Unit) : super(custom) {
        assertSame(Unit, custom)
    }

    companion object : TestInitWithCustomSelectorMeta()
}

fun testAllocNoRetain() {
    // Ensure that calling Kotlin constructor generated for Objective-C initializer doesn't result in
    // redundant retain-release sequence for `alloc` result, since it may provoke specific bugs to reproduce, e.g.
    // the one found in [[NSOutputStream alloc] initToMemory] sequence where initToMemory deallocates its receiver
    // forcibly when replacing it with other object: (to be compiled with ARC enabled)
    /*
    #import <Foundation/Foundation.h>

    void* mem;
    NSOutputStream* allocated = nil;

    int main() {
        allocated = [NSOutputStream alloc];
        NSOutputStream* initialized = [allocated initToMemory];
        mem = calloc(1, 0x10); // To corrupt the 'allocated' object header.
        allocated = nil; // Crashes here in objc_release.

        return 0;
    }
     */

    assertTrue(TestAllocNoRetain().ok)
}

fun testNSOutputStreamToMemoryConstructor() {
    val stream: Any = NSOutputStream(toMemory = Unit)
    assertTrue(stream is NSOutputStream)
}

private const val TestExportObjCClass1Name = "TestExportObjCClass"
@ExportObjCClass(TestExportObjCClass1Name) class TestExportObjCClass1 : NSObject()

@ExportObjCClass class TestExportObjCClass2 : NSObject()

const val TestExportObjCClass34Name = "TestExportObjCClass34"
@ExportObjCClass(TestExportObjCClass34Name) class TestExportObjCClass3 : NSObject()
@ExportObjCClass(TestExportObjCClass34Name) class TestExportObjCClass4 : NSObject()

fun testExportObjCClass() {
    assertEquals(TestExportObjCClass1Name, TestExportObjCClass1().objCClassName)
    assertEquals("TestExportObjCClass2", TestExportObjCClass2().objCClassName)

    assertTrue((TestExportObjCClass3().objCClassName == TestExportObjCClass34Name)
            xor (TestExportObjCClass4().objCClassName == TestExportObjCClass34Name))
}

fun testCustomString() {
    assertFalse(customStringDeallocated)

    fun test() = autoreleasepool {
        val str: String = createCustomString(321)
        assertEquals("321", str)
        assertEquals("CustomString", str.objCClassName)
        assertEquals(321, getCustomStringValue(str))
    }

    test()
    kotlin.native.internal.GC.collect()
    assertTrue(customStringDeallocated)
}

fun testLocalizedStrings() {
    val key = "screen_main_plural_string"
    val localizedString = NSBundle.mainBundle.localizedStringForKey(key, value = "", table = "Localizable")
    val string = NSString.localizedStringWithFormat(localizedString, 5)
    assertEquals("Plural: 5 apples", string)
}

private val Any.objCClassName: String
    get() = object_getClassName(this)!!.toKString()

fun nsArrayOf(vararg elements: Any): NSArray = NSMutableArray().apply {
    elements.forEach {
        this.addObject(it as ObjCObject)
    }
}

fun Any?.asAny(): Any? = this
