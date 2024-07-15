// FIR_IDENTICAL
// ISSUE: KT-69871

// FILE: A.kt
<!WRONG_MODIFIER_TARGET!>public<!> package foo

// FILE: AAA.kt
<!WRONG_MODIFIER_TARGET!>header<!> <!WRONG_MODIFIER_TARGET!>override<!> <!WRONG_MODIFIER_TARGET!>infix<!> <!WRONG_MODIFIER_TARGET!>lateinit<!> <!WRONG_MODIFIER_TARGET!>inline<!> <!WRONG_MODIFIER_TARGET!>reified<!> <!WRONG_MODIFIER_TARGET!>vararg<!> <!WRONG_MODIFIER_TARGET!>enum<!> <!INCOMPATIBLE_MODIFIERS!>const<!> <!WRONG_MODIFIER_TARGET!>out<!> <!WRONG_MODIFIER_TARGET!>tailrec<!> <!WRONG_MODIFIER_TARGET!>internal<!> <!WRONG_MODIFIER_TARGET!>external<!> <!WRONG_MODIFIER_TARGET!>suspend<!> <!WRONG_MODIFIER_TARGET!>value<!> package org.jetbrains.kotlin.package_validator
