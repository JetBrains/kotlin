package test

import kotlin.platform.*

annotation class A(val s: String)

[platformName("bar")]
A("1")
fun foo() = "foo"

A("2")
var v: Int = 1
    [platformName("vget")]
    [A("3")]
    get
    [platformName("vset")]
    [A("4")]
    set
