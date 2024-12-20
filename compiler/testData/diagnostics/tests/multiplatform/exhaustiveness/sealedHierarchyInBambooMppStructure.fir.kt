// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-46031

// MODULE: common-a

expect sealed class Base()
class A : Base() // OK, A in same module with Base

// MODULE: common-b()()(common-a)

class B : Base() // OK, B inherits `expect` class, not `actual`

// MODULE: common-c()()(common-b)

actual sealed class Base actual constructor()
class C : Base() // OK, C in same module with actual Base

// MODULE: common-d()()(common-c)

class D : <!SEALED_INHERITOR_IN_DIFFERENT_MODULE!>Base<!>() // Error, D not in same module with actual Base

// MODULE: jvm()()(common-d)

class E : <!SEALED_INHERITOR_IN_DIFFERENT_MODULE!>Base<!>() // Error, E not in same module with actual Base
