// RUN_PIPELINE_TILL: FRONTEND
// WITH_PLATFORM_LIBS
// Remove this flag once companion blocks are enabled by default.
// LANGUAGE: +CompanionBlocks

// Remove this opt-in once Array.of is stable.
@file:OptIn(kotlin.ExperimentalCollectionLiteralsApi::class)

import platform.darwin.*
import platform.Foundation.*

fun baz(s: Array<Any?>) = NSAssertionHandler().handleFailureInFunction("zzz", "zzz", 0, null)
fun foo(s: Array<Any?>) = NSAssertionHandler().handleFailureInFunction("zzz", "zzz", 0, null, <!VARIADIC_OBJC_SPREAD_IS_SUPPORTED_ONLY_FOR_ARRAYOF!>*s<!>)
fun bar(s: Array<Any?>) = NSAssertionHandler().handleFailureInFunction("zzz", "zzz", 0, null, *arrayOf(*arrayOf(<!VARIADIC_OBJC_SPREAD_IS_SUPPORTED_ONLY_FOR_ARRAYOF!>*s<!>)))
fun arrayDotOf(s: Array<Any?>) = NSAssertionHandler().handleFailureInFunction("zzz", "zzz", 0, null, *Array.of(<!VARIADIC_OBJC_SPREAD_IS_SUPPORTED_ONLY_FOR_ARRAYOF!>*s<!>))
