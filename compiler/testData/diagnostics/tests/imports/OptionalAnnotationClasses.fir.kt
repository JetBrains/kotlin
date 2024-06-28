// WITH_STDLIB

@file:Suppress(<!ERROR_SUPPRESSION!>"OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE"<!>)

import kotlin.js.*
import kotlin.native.concurrent.*

@JsName("")
public fun test() {}

@ThreadLocal
private val EmptyArray: Array<Int> = arrayOf()