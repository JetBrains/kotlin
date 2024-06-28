// WITH_STDLIB
// FULL_JDK
// ISSUE: KT-66784 (similar cases)

import kotlin.jvm.optionals.getOrNull
import java.util.stream.Stream

fun a(s: Stream<Any?>) = s.findAny().<!NONE_APPLICABLE!>getOrNull<!>()
fun b(s: Stream<out Any?>) = s.findAny().getOrNull()
fun c(s: Stream<*>) = s.findAny().getOrNull()

fun d(c: Collection<Any?>) = c.stream().findAny().<!NONE_APPLICABLE!>getOrNull<!>()
fun e(c: Collection<<!REDUNDANT_PROJECTION!>out<!> Any?>) = c.stream().findAny().<!NONE_APPLICABLE!>getOrNull<!>()
fun f(c: Collection<*>) = c.stream().findAny().<!NONE_APPLICABLE!>getOrNull<!>()
