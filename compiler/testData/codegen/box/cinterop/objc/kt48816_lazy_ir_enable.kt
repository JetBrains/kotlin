// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false
// The bug was accidentally fixed with lazy IR for caches, so testing it with this feature enabled, and disabled(in other test file):
// FREE_COMPILER_ARGS: -Xlazy-ir-for-caches=enable

// MODULE: cinterop
// FILE: objclib.def
language = Objective-C
headers = objclib.h
headerFilter = objclib.h **/NSObject.h **/NSDate.h **/NSUUID.h

// FILE: objclib.h
#import <Foundation/NSObject.h>
#import <Foundation/NSDate.h>
#import <Foundation/NSUUID.h>

@interface MyClass1 : NSObject
@end

@interface MyClass2 : NSObject
@end

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import kotlin.reflect.*
import kotlin.test.*
import objclib.*

// https://youtrack.jetbrains.com/issue/KT-48816
open class Base<Ref> {
    operator fun KProperty1<Ref, NSUUID>.getValue(ref: Base<Ref>, property: KProperty<*>): NSUUID? {
        return null
    }
    operator fun KProperty1<Ref, NSDate>.getValue(ref: Base<Ref>, property: KProperty<*>): NSDate? {
        return null
    }
}
class Usage: Base<B>()
class B

// The compilation should fail with the above;
// but anyway try to actually use the code, just to ensure it doesn't get DCEd:

val B.uuid: NSUUID get() = fail()
val B.date: NSDate get() = fail()

fun test1() {
    val uuidProperty = B::uuid
    val dateProperty = B::date
    val usage = Usage()
    with(usage) {
        assertNull(uuidProperty.getValue(usage, uuidProperty))
        assertNull(dateProperty.getValue(usage, dateProperty))
    }
}

// One more reproducer, just in case:
class Property<out R>

open class Base2 {
    fun getValue(property: Property<MyClass1>) = null
    fun getValue(property: Property<MyClass2>) = null
}
class Usage2 : Base2()

fun test2() {
    val usage = Usage2()
    assertNull(usage.getValue(Property<MyClass1>()))
    assertNull(usage.getValue(Property<MyClass2>()))
}

fun box(): String {
    test1()
    test2()
    return "OK"
}