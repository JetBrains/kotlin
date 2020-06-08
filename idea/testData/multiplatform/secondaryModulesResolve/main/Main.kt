// !DIAGNOSTICS: -UNUSED_PARAMETER

package main

import primary.*
import secondary.*
import shared.*

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

    c.<!SECONDARY_DECLARATION_USAGE("secondaryMember", "This reference is declared in module with platform JVM (JVM_1_6), while used in module with platform JVM (JVM_1_6)/JS/Native (general)")!>secondaryMember<!>()
    <!SECONDARY_DECLARATION_USAGE("secondaryTopLevelFun", "This reference is declared in module with platform JVM (JVM_1_6), while used in module with platform JVM (JVM_1_6)/JS/Native (general)")!>secondaryTopLevelFun<!>()
}

fun splitScope(
    a: BothInPrimaryAndSecondary
) {
    a.primaryMember()

    // :(
    a.<!UNRESOLVED_REFERENCE("secondaryMember")!>secondaryMember<!>()
}