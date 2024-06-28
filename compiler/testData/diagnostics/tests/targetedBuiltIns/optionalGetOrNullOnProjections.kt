// WITH_STDLIB
// FULL_JDK
// ISSUE: KT-66784 (similar cases)

import kotlin.jvm.optionals.getOrNull
import java.util.stream.Stream

fun a(s: Stream<Any?>) = s.findAny().<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>getOrNull<!>()
fun b(s: Stream<out Any?>) = s.findAny().getOrNull()
fun c(s: Stream<*>) = s.findAny().getOrNull()

fun d(c: Collection<Any?>) = c.stream().findAny().<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>getOrNull<!>()
fun e(c: Collection<<!REDUNDANT_PROJECTION!>out<!> Any?>) = c.stream().findAny().getOrNull()
fun f(c: Collection<*>) = c.stream().findAny().getOrNull()
