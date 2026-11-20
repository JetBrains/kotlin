// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM
// WITH_STDLIB
// MODULE: lib
// LANGUAGE: +FullValueClasses
// JVM_TARGET: 28
// ENABLE_JVM_PREVIEW
// IGNORE_DEXING
// FILE: lib.kt
package lib

abstract value class IdentityCompiled

// MODULE: main(lib)
// LANGUAGE: +FullValueClasses
// JVM_TARGET: 28
// ENABLE_JVM_PREVIEW
// VALHALLA_VALUE_CLASSES
// FILE: main.kt
import lib.IdentityCompiled

abstract value class SelfCycle : <!CYCLIC_INHERITANCE_HIERARCHY!>SelfCycle<!>()

abstract value class CycleA : <!CYCLIC_INHERITANCE_HIERARCHY!>CycleB<!>()
abstract value class CycleB : <!CYCLIC_INHERITANCE_HIERARCHY!>CycleA<!>()

abstract value class IdentityCycleA : <!CYCLIC_INHERITANCE_HIERARCHY!>IdentityCycleB<!>()
abstract value class IdentityCycleB : <!CYCLIC_INHERITANCE_HIERARCHY!>IdentityCycleA<!>(), <!CYCLIC_INHERITANCE_HIERARCHY!>IdentityCompiled<!>()

value class FromCycle(val x: Int) : CycleA()
value class FromIdentityCycle(val x: Int) : IdentityCycleA()

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor, propertyDeclaration, value */
