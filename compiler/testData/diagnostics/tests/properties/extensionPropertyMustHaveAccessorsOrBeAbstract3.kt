// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-47381

class A

var globalA: A = TODO()

var A.prop get() = this

var i: Int = TODO()

var A.i: Int
    get() = 0