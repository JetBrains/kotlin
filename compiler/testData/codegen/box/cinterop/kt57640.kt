// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: kt57640.def
language = Objective-C
headers = kt57640.h

// FILE:
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

fun main() {
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
