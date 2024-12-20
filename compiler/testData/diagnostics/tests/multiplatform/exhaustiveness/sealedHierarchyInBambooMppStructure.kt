// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-46031

// MODULE: a

expect sealed <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{COMMON}, EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{COMMON}!>class Base<!>()
class A : Base() // OK, A in same module with Base

// MODULE: b()()(a)

class B : Base() // OK, B inherits `expect` class, not `actual`

// MODULE: c()()(b)

actual sealed <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE, EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{COMMON}!>class Base<!> actual constructor()
class C : Base() // OK, C in same module with actual Base

// MODULE: d()()(c)

class D : <!SEALED_INHERITOR_IN_DIFFERENT_MODULE!>Base<!>() // Error, D not in same module with actual Base

// MODULE: main-jvm()()(d)

class E : <!SEALED_INHERITOR_IN_DIFFERENT_MODULE!>Base<!>() // Error, E not in same module with actual Base
