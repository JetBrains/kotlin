// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

package test

interface C1
interface C2
interface R
interface P1
interface P2

context(C1, C2)
fun R.f(p1: P1, p2: P2) {}