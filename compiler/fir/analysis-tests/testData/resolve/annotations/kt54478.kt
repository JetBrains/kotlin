// ISSUE: KT-54478

@file:Suppress(<!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>, "INVISIBLE_MEMBER")

import kotlin.internal.NoInfer

fun <T : Any> test(block: Any.() -> T) {}
fun <T : Any> test(block: @NoInfer T) {}
