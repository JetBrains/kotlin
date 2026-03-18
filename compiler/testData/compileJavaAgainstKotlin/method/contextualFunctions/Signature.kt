// LANGUAGE: +ContextParameters
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR

package test

interface C1
interface C2
interface R
interface P1
interface P2

context(_: C1, _: C2)
fun R.f(p1: P1, p2: P2) {}