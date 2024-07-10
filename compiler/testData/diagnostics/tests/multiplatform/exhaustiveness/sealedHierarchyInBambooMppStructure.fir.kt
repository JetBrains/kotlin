// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-46031

// MODULE: common-a
// TARGET_PLATFORM: Common

expect sealed class Base()
class A : Base() // OK, A in same module with Base

// MODULE: common-b()()(common-a)
// TARGET_PLATFORM: Common

class B : Base() // OK, B inherits `expect` class, not `actual`

// MODULE: common-c()()(common-b)
// TARGET_PLATFORM: Common

actual sealed class Base actual constructor()
class C : Base() // OK, C in same module with actual Base

// MODULE: common-d()()(common-c)
// TARGET_PLATFORM: Common

class D : <!SEALED_INHERITOR_IN_DIFFERENT_MODULE!>Base<!>() // Error, D not in same module with actual Base

// MODULE: jvm()()(common-d)
// TARGET_PLATFORM: JVM

class E : <!SEALED_INHERITOR_IN_DIFFERENT_MODULE!>Base<!>() // Error, E not in same module with actual Base
