package test

import kotlin.platform.*

var v: Int = 1
    [platformName("vget")]
    get
    [platformName("vset")]
    set
