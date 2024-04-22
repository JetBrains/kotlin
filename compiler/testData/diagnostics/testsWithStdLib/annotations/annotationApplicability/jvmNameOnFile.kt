// ISSUE: KT-67101

// FILE: illegal.kt

@file:JvmMultifileClass @file:JvmName("illegal.kt")
package p

val x = 0

fun foo() {}

// FILE: legal.kt

@file:JvmMultifileClass @file:JvmName("legal")
package p

val y = 1

fun bar() {}
