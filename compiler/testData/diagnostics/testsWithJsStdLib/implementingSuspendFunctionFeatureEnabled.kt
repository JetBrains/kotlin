// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +JsAllowImplementingFunctionInterface

abstract class C : suspend () -> Unit {
    override suspend fun invoke() {}
}

abstract class D : () -> Unit {
    override fun invoke() {}
}