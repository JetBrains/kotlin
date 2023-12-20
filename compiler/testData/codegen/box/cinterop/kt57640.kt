// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: kt57640.def
language = Objective-C
headers = kt57640.h

// FILE: kt57640.h
#import <Foundation/NSObject.h>

@interface Base : NSObject
@property (readwrite) Base* delegate;
@end

@protocol Foo
@property (readwrite) id<Foo> delegate;
@end

@protocol Bar
@property (readwrite) id<Bar> delegate;
@end

@interface Derived : Base<Bar, Foo>
// This interface does not have re-declaration of property `delegate`.
// Return type of getter `delegate()` and param type of setter `setDelegate()` are:
//   the type of property defined in the first mentioned protocol (id<Bar>), which is incompatible with property type.
@end

@interface DerivedWithPropertyOverride : Base<Bar, Foo>
// This interface does not have re-declaration of property `delegate`.
// Return type of getter `delegate()` and param type of setter `setDelegate()` are `DerivedWithPropertyOverride*`
@property (readwrite) DerivedWithPropertyOverride* delegate;
@end

// FILE: kt57640.m
#import "kt57640.h"

@implementation Base
@end

@implementation Derived
@end

@implementation DerivedWithPropertyOverride
@end

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import kt57640.*
import kotlin.test.*

class GrandDerived: Derived() {}
class GrandDerivedWithPropertyOverride: DerivedWithPropertyOverride() {}

/**
 * class KotlinInterfaceDerived would cause errors, this deserves a diagnostic test
 * error: class 'KotlinInterfaceDerived' must override 'delegate' because it inherits multiple implementations for it.
 * error: 'delegate' clashes with 'delegate': return types are incompatible.
 * error: 'delegate' clashes with 'delegate': property types do not match.
 */
//class KotlinInterfaceDerived: Base(), FooProtocol, BarProtocol

fun box(): String {
    testBase()

    testAssignmentDerivedToDerived()
    testAssignmentDerivedToBase()
    testAssignmentBaseToDerived()

    testAssignmentDerivedWithPropertyOverrideToDerivedWithPropertyOverride()
    testAssignmentDerivedWithPropertyOverrideToBase()
    testAssignmentBaseToDerivedWithPropertyOverride()

    testGrandDerived()
    testGrandDerivedWithPropertyOverride()
    testAssigmmentDerivedWithPropertyOverrideToGrandDerivedWithPropertyOverride()

    return "OK"
}

private fun testBase() {
    val base = Base()
    val delegate00_Base: Base? = base.delegate
    assertEquals(null, delegate00_Base)
    val delegate01_Base: Base? = base.delegate()
    assertEquals(null, delegate01_Base)

    base.delegate = base
    val delegate02_Base: Base? = base.delegate
    assertEquals(base, delegate02_Base)
    val delegate03_Base: Base? = base.delegate()
    assertEquals(base, delegate03_Base)
}

private fun testAssignmentDerivedToDerived() {
    val derived = Derived()
    val delegate10_Base: Base? = derived.delegate
    assertEquals(null, delegate10_Base)
    val delegate11_Bar: BarProtocol? = derived.delegate()
    assertEquals(null, delegate11_Bar)

    derived.delegate = derived
    derived.setDelegate(derived)

    val delegate12_Base: Base? = derived.delegate
    assertTrue(delegate12_Base is Base)
    assertTrue(delegate12_Base is FooProtocol)
    assertTrue(delegate12_Base is BarProtocol)
    assertTrue(delegate12_Base is Derived)
    assertEquals(derived, delegate12_Base)
    val delegate16_Bar: BarProtocol? = derived.delegate()
    assertTrue(delegate16_Bar is Base)
    assertTrue(delegate16_Bar is FooProtocol)
    assertTrue(delegate16_Bar is BarProtocol)
    assertTrue(delegate16_Bar is Derived)
    assertEquals(derived, delegate16_Bar)

    val delegate13_Base: Base? = (derived as Base).delegate
    assertTrue(delegate13_Base is Base)
    assertTrue(delegate13_Base is FooProtocol)
    assertTrue(delegate13_Base is BarProtocol)
    assertTrue(delegate13_Base is Derived)
    assertEquals(derived, delegate13_Base)
    val delegate17_Base: Base? = (derived as Base).delegate()
    assertTrue(delegate17_Base is Base)
    assertTrue(delegate17_Base is FooProtocol)
    assertTrue(delegate17_Base is BarProtocol)
    assertTrue(delegate17_Base is Derived)
    assertEquals(derived, delegate17_Base)

    val delegate14_Foo: FooProtocol? = (derived as FooProtocol).delegate
    assertTrue(delegate14_Foo is Base)
    assertTrue(delegate14_Foo is FooProtocol)
    assertTrue(delegate14_Foo is BarProtocol)
    assertTrue(delegate14_Foo is Derived)
    assertEquals(derived, delegate14_Foo)
    val delegate18_Foo: FooProtocol? = (derived as FooProtocol).delegate()
    assertTrue(delegate18_Foo is Base)
    assertTrue(delegate18_Foo is FooProtocol)
    assertTrue(delegate18_Foo is BarProtocol)
    assertTrue(delegate18_Foo is Derived)
    assertEquals(derived, delegate18_Foo)

    val delegate15_Bar: BarProtocol? = (derived as BarProtocol).delegate
    assertTrue(delegate15_Bar is Base)
    assertTrue(delegate15_Bar is FooProtocol)
    assertTrue(delegate15_Bar is BarProtocol)
    assertTrue(delegate15_Bar is Derived)
    assertEquals(derived, delegate15_Bar)
    val delegate19_Bar: BarProtocol? = (derived as BarProtocol).delegate()
    assertTrue(delegate19_Bar is Base)
    assertTrue(delegate19_Bar is FooProtocol)
    assertTrue(delegate19_Bar is BarProtocol)
    assertTrue(delegate19_Bar is Derived)
    assertEquals(derived, delegate19_Bar)
}

private fun testAssignmentDerivedToBase() {
    val base = Base()
    val delegate10_Base: Base? = base.delegate
    assertEquals(null, delegate10_Base)
    val delegate11_Base: Base? = base.delegate()
    assertEquals(null, delegate11_Base)

    base.delegate = base
    base.setDelegate(base)
    val derived = Derived()
    base.delegate = derived

    val delegate12_Base: Base? = base.delegate
    assertTrue(delegate12_Base is Base)
    assertTrue(delegate12_Base is FooProtocol)
    assertTrue(delegate12_Base is BarProtocol)
    assertTrue(delegate12_Base is Derived)
    assertEquals(derived, delegate12_Base)
    val delegate16_Base: Base? = base.delegate()
    assertTrue(delegate16_Base is Base)
    assertTrue(delegate16_Base is FooProtocol)
    assertTrue(delegate16_Base is BarProtocol)
    assertTrue(delegate16_Base is Derived)
    assertEquals(derived, delegate16_Base)
}

private fun testAssignmentBaseToDerived() {
    val derived = Derived()
    val delegate10_Base: Base? = derived.delegate
    assertEquals(null, delegate10_Base)
    val delegate11_Bar: BarProtocol? = derived.delegate()
    assertEquals(null, delegate11_Bar)

    val base = Base()
    derived.delegate = base
//    derived.setDelegate(base) // argument type mismatch, this deserves a diagnostic test

    val delegate12_Base: Base? = derived.delegate
    assertTrue(delegate12_Base is Base)
    assertFalse(delegate12_Base is FooProtocol)
    assertFalse(delegate12_Base is BarProtocol)
    assertFalse(delegate12_Base is Derived)
    assertEquals(base, delegate12_Base)
    val delegate16_Bar: BarProtocol? = derived.delegate()
    val delegate16_Any: Any? = delegate16_Bar
    assertTrue(delegate16_Bar is Base)
    assertTrue(delegate16_Any is Base)
    assertFalse(delegate16_Bar is FooProtocol)
    assertFalse(delegate16_Any is FooProtocol)
// Behavior of next "is" check depends on existence of static type analysis: static analysis calculates "is" result as "true",
// while at run-time, the type is Base, which isn't BarProtocol, and the "is" result is "false"
//    assertFalse(delegate16_Bar is BarProtocol)
    assertFalse(delegate16_Any is BarProtocol)  // actual type is Base, which isn't BarProtocol
    assertFalse(delegate16_Bar is Derived)
    assertFalse(delegate16_Any is Derived)
    assertEquals(base, delegate16_Bar)
    assertEquals(base, delegate16_Any)

    val delegate13_Base: Base? = (derived as Base).delegate
    assertTrue(delegate13_Base is Base)
    assertFalse(delegate13_Base is FooProtocol)
    assertFalse(delegate13_Base is BarProtocol)
    assertFalse(delegate13_Base is Derived)
    assertEquals(base, delegate13_Base)
    val delegate17_Base: Base? = (derived as Base).delegate()
    assertTrue(delegate17_Base is Base)
    assertFalse(delegate17_Base is FooProtocol)
    assertFalse(delegate17_Base is BarProtocol)
    assertFalse(delegate17_Base is Derived)
    assertEquals(base, delegate17_Base)

    val delegate14_Foo: FooProtocol? = (derived as FooProtocol).delegate
    val delegate14_Any: Any? = delegate14_Foo
    assertTrue(delegate14_Foo is Base)
    assertTrue(delegate14_Any is Base)
// Behavior of next "is" check depends on existence of static type analysis: static analysis calculates "is" result as "true",
// while at run-time, the type is Base, which isn't FooProtocol, and the "is" result is "false"
//    assertFalse(delegate14_Foo is FooProtocol)
    assertFalse(delegate14_Any is FooProtocol) // actual type is Base, which isn't FooProtocol
    assertFalse(delegate14_Foo is BarProtocol)
    assertFalse(delegate14_Any is BarProtocol)
    assertFalse(delegate14_Foo is Derived)
    assertEquals(base, delegate14_Foo as Base)
    assertEquals(base, delegate14_Any as Base)

    val delegate18_Foo: FooProtocol? = (derived as FooProtocol).delegate()
    val delegate18_Any: Any? = delegate18_Foo
    assertTrue(delegate18_Foo is Base)
    assertTrue(delegate18_Any is Base)
// Behavior of next "is" check depends on existence of static type analysis: static analysis calculates "is" result as "true",
// while at run-time, the type is Base, which isn't FooProtocol, and the "is" result is "false"
//    assertFalse(delegate18_Foo is FooProtocol)
    assertFalse(delegate18_Any is FooProtocol) // actual type is Base, which isn't FooProtocol
    assertFalse(delegate18_Foo is BarProtocol)
    assertFalse(delegate18_Any is BarProtocol)
    assertFalse(delegate18_Foo is Derived)
    assertFalse(delegate18_Any is Derived)
    assertEquals(base, delegate18_Foo)
    assertEquals(base, delegate18_Any)

    val delegate15_Bar: BarProtocol? = (derived as BarProtocol).delegate
    val delegate15_Any: Any? = delegate15_Bar
    assertTrue(delegate15_Bar is Base)
    assertTrue(delegate15_Any is Base)
    assertFalse(delegate15_Bar is FooProtocol)
    assertFalse(delegate15_Any is FooProtocol)
// Behavior of next "is" check depends on existence of static type analysis: static analysis calculates "is" result as "true",
// while at run-time, the type is Base, which isn't BarProtocol, and the "is" result is "false"
//    assertFalse(delegate15_Bar is BarProtocol)
    assertFalse(delegate15_Any is BarProtocol) // actual type is Base, which isn't BarProtocol
    assertFalse(delegate15_Bar is Derived)
    assertFalse(delegate15_Any is Derived)
    assertEquals(base, delegate15_Bar as Base)
    assertEquals(base, delegate15_Any as Base)

    val delegate19_Bar: BarProtocol? = (derived as BarProtocol).delegate()
    val delegate19_Any: Any? = delegate19_Bar
    assertTrue(delegate19_Bar is Base)
    assertTrue(delegate19_Any is Base)
    assertFalse(delegate19_Bar is FooProtocol)
    assertFalse(delegate19_Any is FooProtocol)
// Behavior of next "is" check depends on existence of static type analysis: static analysis calculates "is" result as "true",
// while at run-time, the type is Base, which isn't BarProtocol, and the "is" result is "false"
//    assertFalse(delegate19_Bar is BarProtocol)
    assertFalse(delegate19_Any is BarProtocol) // actual type is Base, which isn't BarProtocol
    assertFalse(delegate19_Bar is Derived)
    assertFalse(delegate19_Any is Derived)
    assertEquals(base, delegate19_Bar)
    assertEquals(base, delegate19_Any)
}

private fun testAssignmentDerivedWithPropertyOverrideToDerivedWithPropertyOverride() {
    val derived = DerivedWithPropertyOverride()
    // WARNING: static type of backing field in CInterop KLib is `Base?`, but not expected `DerivedWithPropertyOverride?`. Comments below are related
    val delegate10_Base: Base? = derived.delegate
    assertEquals(null, delegate10_Base)
    val delegate11_Bar: BarProtocol? = derived.delegate()
    assertEquals(null, delegate11_Bar)

    derived.delegate = derived
    derived.setDelegate(derived)

    val delegate12_Base: Base? = derived.delegate // static type is Base?, not DerivedWithPropertyOverride?, this deserves a diagnostic test
    assertTrue(delegate12_Base is Base)
    assertTrue(delegate12_Base is FooProtocol)
    assertTrue(delegate12_Base is BarProtocol)
    assertTrue(delegate12_Base is DerivedWithPropertyOverride)
    assertEquals(derived, delegate12_Base)
    val delegate19_DerivedWithPropertyOverride: DerivedWithPropertyOverride? = derived.delegate()
    assertTrue(delegate19_DerivedWithPropertyOverride is Base)
    assertTrue(delegate19_DerivedWithPropertyOverride is FooProtocol)
    assertTrue(delegate19_DerivedWithPropertyOverride is BarProtocol)
    assertTrue(delegate19_DerivedWithPropertyOverride is DerivedWithPropertyOverride)
    assertEquals(derived, delegate19_DerivedWithPropertyOverride)

    val delegate13_Base: Base? = (derived as Base).delegate
    assertTrue(delegate13_Base is Base)
    assertTrue(delegate13_Base is FooProtocol)
    assertTrue(delegate13_Base is BarProtocol)
    assertTrue(delegate13_Base is DerivedWithPropertyOverride)
    assertEquals(derived, delegate13_Base)
    val delegate17_Base: Base? = (derived as Base).delegate() // static type is Base?, not DerivedWithPropertyOverride?, this deserves a diagnostic test
    assertTrue(delegate17_Base is Base)
    assertTrue(delegate17_Base is FooProtocol)
    assertTrue(delegate17_Base is BarProtocol)
    assertTrue(delegate17_Base is DerivedWithPropertyOverride)
    assertEquals(derived, delegate17_Base)

    val delegate14_Foo: FooProtocol? = (derived as FooProtocol).delegate
    assertTrue(delegate14_Foo is Base)
    assertTrue(delegate14_Foo is FooProtocol)
    assertTrue(delegate14_Foo is BarProtocol)
    assertTrue(delegate14_Foo is DerivedWithPropertyOverride)
    assertEquals(derived, delegate14_Foo)
    val delegate18_Foo: FooProtocol? = (derived as FooProtocol).delegate() // static type is FooProtocol?, not DerivedWithPropertyOverride?, this deserves a diagnostic test
    assertTrue(delegate18_Foo is Base)
    assertTrue(delegate18_Foo is FooProtocol)
    assertTrue(delegate18_Foo is BarProtocol)
    assertTrue(delegate18_Foo is DerivedWithPropertyOverride)
    assertEquals(derived, delegate18_Foo)

    val delegate15_Bar: BarProtocol? = (derived as BarProtocol).delegate
    assertTrue(delegate15_Bar is Base)
    assertTrue(delegate15_Bar is FooProtocol)
    assertTrue(delegate15_Bar is BarProtocol)
    assertTrue(delegate15_Bar is DerivedWithPropertyOverride)
    assertEquals(derived, delegate15_Bar)
    val delegate19_Bar: BarProtocol? = (derived as BarProtocol).delegate() // static type is BarProtocol?, not DerivedWithPropertyOverride?, this deserves a diagnostic test
    assertTrue(delegate19_Bar is Base)
    assertTrue(delegate19_Bar is FooProtocol)
    assertTrue(delegate19_Bar is BarProtocol)
    assertTrue(delegate19_Bar is DerivedWithPropertyOverride)
    assertEquals(derived, delegate19_Bar)
}

private fun testAssignmentDerivedWithPropertyOverrideToBase() {
    val base = Base()
    val delegate10_Base: Base? = base.delegate
    assertEquals(null, delegate10_Base)
    val delegate11_Base: Base? = base.delegate()
    assertEquals(null, delegate11_Base)

    val derived = DerivedWithPropertyOverride()
    base.delegate = derived
    base.setDelegate(derived)

    val delegate12_Base: Base? = base.delegate
    assertTrue(delegate12_Base is Base)
    assertTrue(delegate12_Base is FooProtocol)
    assertTrue(delegate12_Base is BarProtocol)
    assertTrue(delegate12_Base is DerivedWithPropertyOverride)
    assertEquals(derived, delegate12_Base)
    val delegate19_Base: Base? = base.delegate()
    assertTrue(delegate19_Base is Base)
    assertTrue(delegate19_Base is FooProtocol)
    assertTrue(delegate19_Base is BarProtocol)
    assertTrue(delegate19_Base is DerivedWithPropertyOverride)
    assertEquals(derived, delegate19_Base)
}

private fun testAssignmentBaseToDerivedWithPropertyOverride() {
    val derived = DerivedWithPropertyOverride()
    // WARNING: static type of backing field in CInterop KLib is `Base?`, but not expected `DerivedWithPropertyOverride?`. Comments below are related
    val delegate10_Base: Base? = derived.delegate
    assertEquals(null, delegate10_Base)
    val delegate11_Bar: BarProtocol? = derived.delegate()
    assertEquals(null, delegate11_Bar)

    val base = Base()
    derived.delegate = base
//    derived.setDelegate(base) //  argument type mismatch: actual type is 'library.Base', but 'library.DerivedWithPropertyOverride?' was expected, this deserves a diagnostic test

    val delegate12_Base: Base? = derived.delegate // static type is Base?, not DerivedWithPropertyOverride?, this deserves a diagnostic test
    assertTrue(delegate12_Base is Base)
    assertFalse(delegate12_Base is FooProtocol)
    assertFalse(delegate12_Base is BarProtocol)
    assertFalse(delegate12_Base is DerivedWithPropertyOverride)
    assertEquals(base, delegate12_Base)
    val delegate19_DerivedWithPropertyOverride: DerivedWithPropertyOverride? = derived.delegate()
    assertTrue(delegate19_DerivedWithPropertyOverride is Base)
    assertTrue(delegate19_DerivedWithPropertyOverride is FooProtocol)
    assertTrue(delegate19_DerivedWithPropertyOverride is BarProtocol)
    assertTrue(delegate19_DerivedWithPropertyOverride is DerivedWithPropertyOverride)
    assertEquals(base, delegate19_DerivedWithPropertyOverride)

    val delegate13_Base: Base? = (derived as Base).delegate
    assertTrue(delegate13_Base is Base)
    assertFalse(delegate13_Base is FooProtocol)
    assertFalse(delegate13_Base is BarProtocol)
    assertFalse(delegate13_Base is DerivedWithPropertyOverride)
    assertEquals(base, delegate13_Base)
    val delegate17_Base: Base? = (derived as Base).delegate() // static type is Base?, not DerivedWithPropertyOverride?, this deserves a diagnostic test
    assertTrue(delegate17_Base is Base)
    assertFalse(delegate17_Base is FooProtocol)
    assertFalse(delegate17_Base is BarProtocol)
    assertFalse(delegate17_Base is DerivedWithPropertyOverride)
    assertEquals(base, delegate17_Base)

    val delegate14_Foo: FooProtocol? = (derived as FooProtocol).delegate
    assertTrue(delegate14_Foo is Base)
// Behavior of next "is" check depends on existence of static type analysis: static analysis calculates "is" result as "true",
// while at run-time, the type is Base, which isn't FooProtocol, and the "is" result is "false"
//    assertFalse(delegate14_Foo is FooProtocol)
    assertFalse(delegate14_Foo is BarProtocol)
    assertFalse(delegate14_Foo is DerivedWithPropertyOverride)
    assertEquals(base, delegate14_Foo)
    val delegate18_Foo: FooProtocol? = (derived as FooProtocol).delegate() // static type is FooProtocol?, not DerivedWithPropertyOverride?, this deserves a diagnostic test
    assertTrue(delegate18_Foo is Base)
// Behavior of next "is" check depends on existence of static type analysis: static analysis calculates "is" result as "true",
// while at run-time, the type is Base, which isn't FooProtocol, and the "is" result is "false"
//    assertFalse(delegate18_Foo is FooProtocol)
    assertFalse(delegate18_Foo is BarProtocol)
    assertFalse(delegate18_Foo is DerivedWithPropertyOverride)
    assertEquals(base, delegate18_Foo)

    val delegate15_Bar: BarProtocol? = (derived as BarProtocol).delegate
    assertTrue(delegate15_Bar is Base)
    assertFalse(delegate15_Bar is FooProtocol)
// Behavior of next "is" check depends on existence of static type analysis: static analysis calculates "is" result as "true",
// while at run-time, the type is Base, which isn't BarProtocol, and the "is" result is "false"
//    assertFalse(delegate15_Bar is BarProtocol)
    assertFalse(delegate15_Bar is DerivedWithPropertyOverride)
    assertEquals(base, delegate15_Bar)
    val delegate19_Bar: BarProtocol? = (derived as BarProtocol).delegate() // static type is BarProtocol?, not DerivedWithPropertyOverride?, this deserves a diagnostic test
    assertTrue(delegate19_Bar is Base)
    assertFalse(delegate19_Bar is FooProtocol)
// Behavior of next "is" check depends on existence of static type analysis: static analysis calculates "is" result as "true",
// while at run-time, the type is Base, which isn't BarProtocol, and the "is" result is "false"
//    assertFalse(delegate19_Bar is BarProtocol)
    assertFalse(delegate19_Bar is DerivedWithPropertyOverride)
    assertEquals(base, delegate19_Bar)
}

private fun testGrandDerived() {
    val grandDerived = GrandDerived()
    val delegate10_Base: Base? = grandDerived.delegate
    assertEquals(null, delegate10_Base)
    val delegate11_Bar: BarProtocol? = grandDerived.delegate()
    assertEquals(null, delegate11_Bar)

    grandDerived.delegate = Base()
//    grandDerived.setDelegate(Base()) // argument type mismatch: actual type is 'library.Base', but 'library.BarProtocol?' was expected, this deserves a diagnostic test
    grandDerived.delegate = Derived()
    grandDerived.setDelegate(Derived())
    grandDerived.delegate = grandDerived
    grandDerived.setDelegate(grandDerived)

    val delegate12_Base: Base? = grandDerived.delegate
    assertTrue(delegate12_Base is Base)
    assertTrue(delegate12_Base is FooProtocol)
    assertTrue(delegate12_Base is BarProtocol)
    assertTrue(delegate12_Base is Derived)
    assertTrue(delegate12_Base is GrandDerived)
    assertEquals(grandDerived, delegate12_Base)
    val delegate21_Bar: BarProtocol? = grandDerived.delegate()
    assertTrue(delegate21_Bar is Base)
    assertTrue(delegate21_Bar is FooProtocol)
    assertTrue(delegate21_Bar is BarProtocol)
    assertTrue(delegate21_Bar is Derived)
    assertTrue(delegate21_Bar is GrandDerived)
    assertEquals(grandDerived, delegate21_Bar)

    val delegate13_Base: Base? = (grandDerived as Base).delegate
    assertTrue(delegate13_Base is Base)
    assertTrue(delegate13_Base is FooProtocol)
    assertTrue(delegate13_Base is BarProtocol)
    assertTrue(delegate13_Base is Derived)
    assertTrue(delegate13_Base is GrandDerived)
    assertEquals(grandDerived, delegate13_Base)
    val delegate17_Base: Base? = (grandDerived as Base).delegate()
    assertTrue(delegate17_Base is Base)
    assertTrue(delegate17_Base is FooProtocol)
    assertTrue(delegate17_Base is BarProtocol)
    assertTrue(delegate17_Base is Derived)
    assertTrue(delegate17_Base is GrandDerived)
    assertEquals(grandDerived, delegate17_Base)

    val delegate14_Foo: FooProtocol? = (grandDerived as FooProtocol).delegate
    assertTrue(delegate14_Foo is Base)
    assertTrue(delegate14_Foo is FooProtocol)
    assertTrue(delegate14_Foo is BarProtocol)
    assertTrue(delegate14_Foo is Derived)
    assertTrue(delegate14_Foo is GrandDerived)
    assertEquals(grandDerived, delegate14_Foo)
    val delegate18_Foo: FooProtocol? = (grandDerived as FooProtocol).delegate()
    assertTrue(delegate18_Foo is Base)
    assertTrue(delegate18_Foo is FooProtocol)
    assertTrue(delegate18_Foo is BarProtocol)
    assertTrue(delegate18_Foo is Derived)
    assertTrue(delegate18_Foo is GrandDerived)
    assertEquals(grandDerived, delegate18_Foo)

    val delegate15_Bar: BarProtocol? = (grandDerived as BarProtocol).delegate
    assertTrue(delegate15_Bar is Base)
    assertTrue(delegate15_Bar is FooProtocol)
    assertTrue(delegate15_Bar is BarProtocol)
    assertTrue(delegate15_Bar is Derived)
    assertTrue(delegate15_Bar is GrandDerived)
    assertEquals(grandDerived, delegate15_Bar)
    val delegate19_Bar: BarProtocol? = (grandDerived as BarProtocol).delegate()
    assertTrue(delegate19_Bar is Base)
    assertTrue(delegate19_Bar is FooProtocol)
    assertTrue(delegate19_Bar is BarProtocol)
    assertTrue(delegate19_Bar is Derived)
    assertTrue(delegate19_Bar is GrandDerived)
    assertEquals(grandDerived, delegate19_Bar)

    val delegate16_Base: Base? = (grandDerived as Derived).delegate
    assertTrue(delegate16_Base is Base)
    assertTrue(delegate16_Base is FooProtocol)
    assertTrue(delegate16_Base is BarProtocol)
    assertTrue(delegate16_Base is Derived)
    assertTrue(delegate16_Base is GrandDerived)
    assertEquals(grandDerived, delegate16_Base)
    val delegate20_Bar: BarProtocol? = (grandDerived as Derived).delegate()
    assertTrue(delegate20_Bar is Base)
    assertTrue(delegate20_Bar is FooProtocol)
    assertTrue(delegate20_Bar is BarProtocol)
    assertTrue(delegate20_Bar is Derived)
    assertTrue(delegate20_Bar is GrandDerived)
    assertEquals(grandDerived, delegate20_Bar)
}

private fun testGrandDerivedWithPropertyOverride() {
    val grandDerived = GrandDerivedWithPropertyOverride()
    val delegate10_Base: Base? = grandDerived.delegate
    assertEquals(null, delegate10_Base)
    val delegate11_Bar: BarProtocol? = grandDerived.delegate()
    assertEquals(null, delegate11_Bar)

    grandDerived.delegate = Base()
//    grandDerived.setDelegate(Base()) // argument type mismatch: actual type is 'library.Base', but 'library.DerivedWithPropertyOverride?' was expected, this deserves a diagnostic test
    grandDerived.delegate = Derived()
//    grandDerived.setDelegate(Derived()) // argument type mismatch: actual type is 'library.Derived', but 'library.DerivedWithPropertyOverride?' was expected, this deserves a diagnostic test
    grandDerived.delegate = DerivedWithPropertyOverride()
    grandDerived.setDelegate(DerivedWithPropertyOverride())
    grandDerived.delegate = grandDerived
    grandDerived.setDelegate(grandDerived)

    val delegate12_Base: Base? = grandDerived.delegate
    assertTrue(delegate12_Base is Base)
    assertTrue(delegate12_Base is FooProtocol)
    assertTrue(delegate12_Base is BarProtocol)
    assertTrue(delegate12_Base is DerivedWithPropertyOverride)
    assertTrue(delegate12_Base is GrandDerivedWithPropertyOverride)
    assertEquals(grandDerived, delegate12_Base)

    val delegate13_Base: Base? = (grandDerived as Base).delegate
    assertTrue(delegate13_Base is Base)
    assertTrue(delegate13_Base is FooProtocol)
    assertTrue(delegate13_Base is BarProtocol)
    assertTrue(delegate13_Base is DerivedWithPropertyOverride)
    assertTrue(delegate13_Base is GrandDerivedWithPropertyOverride)
    assertEquals(grandDerived, delegate13_Base)

    val delegate14_Foo: FooProtocol? = (grandDerived as FooProtocol).delegate
    assertTrue(delegate14_Foo is Base)
    assertTrue(delegate14_Foo is FooProtocol)
    assertTrue(delegate14_Foo is BarProtocol)
    assertTrue(delegate14_Foo is DerivedWithPropertyOverride)
    assertTrue(delegate14_Foo is GrandDerivedWithPropertyOverride)
    assertEquals(grandDerived, delegate14_Foo)

    val delegate15_Bar: BarProtocol? = (grandDerived as BarProtocol).delegate
    assertTrue(delegate15_Bar is Base)
    assertTrue(delegate15_Bar is FooProtocol)
    assertTrue(delegate15_Bar is BarProtocol)
    assertTrue(delegate15_Bar is DerivedWithPropertyOverride)
    assertTrue(delegate15_Bar is GrandDerivedWithPropertyOverride)
    assertEquals(grandDerived, delegate15_Bar)

    val delegate16_DerivedWithPropertyOverride: DerivedWithPropertyOverride? = grandDerived.delegate()
    assertTrue(delegate16_DerivedWithPropertyOverride is Base)
    assertTrue(delegate16_DerivedWithPropertyOverride is FooProtocol)
    assertTrue(delegate16_DerivedWithPropertyOverride is BarProtocol)
    assertTrue(delegate16_DerivedWithPropertyOverride is DerivedWithPropertyOverride)
    assertTrue(delegate16_DerivedWithPropertyOverride is GrandDerivedWithPropertyOverride)
    assertEquals(grandDerived, delegate16_DerivedWithPropertyOverride)

    val delegate17_Base: Base? = (grandDerived as Base).delegate()
    assertTrue(delegate17_Base is Base)
    assertTrue(delegate17_Base is FooProtocol)
    assertTrue(delegate17_Base is BarProtocol)
    assertTrue(delegate17_Base is DerivedWithPropertyOverride)
    assertTrue(delegate17_Base is GrandDerivedWithPropertyOverride)
    assertEquals(grandDerived, delegate17_Base)

    val delegate18_FooProtocol: FooProtocol? = (grandDerived as FooProtocol).delegate()
    assertTrue(delegate18_FooProtocol is Base)
    assertTrue(delegate18_FooProtocol is FooProtocol)
    assertTrue(delegate18_FooProtocol is BarProtocol)
    assertTrue(delegate18_FooProtocol is DerivedWithPropertyOverride)
    assertTrue(delegate18_FooProtocol is GrandDerivedWithPropertyOverride)
    assertEquals(grandDerived, delegate18_FooProtocol)

    val delegate19_BarProtocol: BarProtocol? = (grandDerived as BarProtocol).delegate()
    assertTrue(delegate19_BarProtocol is Base)
    assertTrue(delegate19_BarProtocol is FooProtocol)
    assertTrue(delegate19_BarProtocol is BarProtocol)
    assertTrue(delegate19_BarProtocol is DerivedWithPropertyOverride)
    assertTrue(delegate19_BarProtocol is GrandDerivedWithPropertyOverride)
    assertEquals(grandDerived, delegate19_BarProtocol)

    val delegate20_DerivedWithPropertyOverride: DerivedWithPropertyOverride? = (grandDerived as DerivedWithPropertyOverride).delegate()
    assertTrue(delegate20_DerivedWithPropertyOverride is Base)
    assertTrue(delegate20_DerivedWithPropertyOverride is FooProtocol)
    assertTrue(delegate20_DerivedWithPropertyOverride is BarProtocol)
    assertTrue(delegate20_DerivedWithPropertyOverride is DerivedWithPropertyOverride)
    assertTrue(delegate20_DerivedWithPropertyOverride is GrandDerivedWithPropertyOverride)
    assertEquals(grandDerived, delegate20_DerivedWithPropertyOverride)
}

private fun testAssigmmentDerivedWithPropertyOverrideToGrandDerivedWithPropertyOverride() {
    val grandDerived = GrandDerivedWithPropertyOverride()
    val delegate10_Base: Base? = grandDerived.delegate
    assertEquals(null, delegate10_Base)
    val delegate11_Bar: BarProtocol? = grandDerived.delegate()
    assertEquals(null, delegate11_Bar)

    val derived = DerivedWithPropertyOverride()
    grandDerived.delegate = derived
    grandDerived.setDelegate(derived) // argument type mismatch: actual type is 'library.Base', but 'library.DerivedWithPropertyOverride?' was expected, this deserves a diagnostic test

    val delegate12_Base: Base? = grandDerived.delegate
    assertTrue(delegate12_Base is Base)
    assertTrue(delegate12_Base is FooProtocol)
    assertTrue(delegate12_Base is BarProtocol)
    assertTrue(delegate12_Base is DerivedWithPropertyOverride)
    assertFalse(delegate12_Base is GrandDerivedWithPropertyOverride)
    assertEquals(derived, delegate12_Base)

    val delegate13_Base: Base? = (grandDerived as Base).delegate
    assertTrue(delegate13_Base is Base)
    assertTrue(delegate13_Base is FooProtocol)
    assertTrue(delegate13_Base is BarProtocol)
    assertTrue(delegate13_Base is DerivedWithPropertyOverride)
    assertFalse(delegate13_Base is GrandDerivedWithPropertyOverride)
    assertEquals(derived, delegate13_Base)

    val delegate14_Foo: FooProtocol? = (grandDerived as FooProtocol).delegate
    assertTrue(delegate14_Foo is Base)
    assertTrue(delegate14_Foo is FooProtocol)
    assertTrue(delegate14_Foo is BarProtocol)
    assertTrue(delegate14_Foo is DerivedWithPropertyOverride)
    assertFalse(delegate14_Foo is GrandDerivedWithPropertyOverride)
    assertEquals(derived, delegate14_Foo)

    val delegate15_Bar: BarProtocol? = (grandDerived as BarProtocol).delegate
    assertTrue(delegate15_Bar is Base)
    assertTrue(delegate15_Bar is FooProtocol)
    assertTrue(delegate15_Bar is BarProtocol)
    assertTrue(delegate15_Bar is DerivedWithPropertyOverride)
    assertFalse(delegate15_Bar is GrandDerivedWithPropertyOverride)
    assertEquals(derived, delegate15_Bar)

    val delegate16_DerivedWithPropertyOverride: DerivedWithPropertyOverride? = (grandDerived as DerivedWithPropertyOverride).delegate()
    assertTrue(delegate16_DerivedWithPropertyOverride is Base)
    assertTrue(delegate16_DerivedWithPropertyOverride is FooProtocol)
    assertTrue(delegate16_DerivedWithPropertyOverride is BarProtocol)
    assertTrue(delegate16_DerivedWithPropertyOverride is DerivedWithPropertyOverride)
    assertFalse(delegate16_DerivedWithPropertyOverride is GrandDerivedWithPropertyOverride)
    assertEquals(derived, delegate16_DerivedWithPropertyOverride)
}
