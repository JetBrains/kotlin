// RUN_PIPELINE_TILL: FRONTEND
// WITH_PLATFORM_LIBS
// Remove this flag once companion blocks are enabled by default.
// LANGUAGE: +CompanionBlocks

// Remove this opt-in once Array.of is stable.
@file:OptIn(kotlin.ExperimentalCollectionLiteralsApi::class)

import platform.darwin.*
import platform.Foundation.*

fun baz(s: Array<Any?>) = NSLog("zzz")
fun foo(s: Array<Any?>) = NSLog("zzz", <!VARIADIC_C_SPREAD_IS_SUPPORTED_ONLY_FOR_ARRAYOF!>*s<!>)
fun bar(s: Array<Any?>) = NSLog("zzz", *arrayOf(*arrayOf(<!VARIADIC_C_SPREAD_IS_SUPPORTED_ONLY_FOR_ARRAYOF!>*s<!>)))
fun arrayDotOf(s: Array<Any?>) = NSLog("zzz", *Array.of(<!VARIADIC_C_SPREAD_IS_SUPPORTED_ONLY_FOR_ARRAYOF!>*s<!>))
