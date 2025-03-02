// RUN_PIPELINE_TILL: FRONTEND
// SKIP_JAVAC
// WITH_STDLIB

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class IC(val s: String)

@JvmExposeBoxed("foo")
@JvmName("foo")
fun bar1(ic: IC) {}

@JvmExposeBoxed("foo")
@JvmName("foo")
fun barIC(): IC = TODO()

@JvmExposeBoxed
@JvmName("foo")
fun bar2(ic: IC): IC = TODO()

@JvmExposeBoxed
@JvmName("foo")
fun barIC2(): IC = TODO()

