// RUN_PIPELINE_TILL: CODEGEN
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import kotlinx.cinterop.*

fun foo(x: CValue<*>?) = x

fun bar() {
    staticCFunction(::foo)
}
