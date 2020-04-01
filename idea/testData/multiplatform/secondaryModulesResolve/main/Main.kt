// !DIAGNOSTICS: -UNUSED_PARAMETER

package main

import primary.*
import secondary.*

fun usageInSignature(
    a: PrimaryClass,
    b: PrimaryClass.PrimaryNestedClass,
    c: SecondaryClass,
    e: SecondaryClass.SecondaryNestedClass
) { }

fun directCalls(
    a: PrimaryClass,
    b: PrimaryClass.PrimaryNestedClass,
    c: SecondaryClass,
    e: SecondaryClass.SecondaryNestedClass
) {
    a.primaryMember()
    primaryTopLevelFun()

    c.<!SECONDARY_DECLARATION_USAGE("secondaryMember", "This reference is declared in module with platform JVM (JVM_1_6), while used in module with platform JVM (JVM_1_6)/JS/Native")!>secondaryMember<!>()
    <!SECONDARY_DECLARATION_USAGE("secondaryTopLevelFun", "This reference is declared in module with platform JVM (JVM_1_6), while used in module with platform JVM (JVM_1_6)/JS/Native")!>secondaryTopLevelFun<!>()
}
