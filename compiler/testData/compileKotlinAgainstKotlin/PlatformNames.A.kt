package lib

import kotlin.platform.*

[platformName("bar")]
fun foo() = "foo"

var v: Int = 1
    [platformName("vget")]
    get
    [platformName("vset")]
    set
