// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73256
// LANGUAGE: +AnnotationAllUseSiteTarget
// FIR_DUMP

annotation class A1

annotation class A2

class My(@all:[A1 A2] val x: Int, @property:[A1 A2] val y: Int)
