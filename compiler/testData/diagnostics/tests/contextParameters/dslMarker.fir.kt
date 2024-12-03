// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// DIAGNOSTICS: -UNSUPPORTED_FEATURE, -CONTEXT_RECEIVERS_DEPRECATED

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class Dsl

@Dsl
class DslReceiver {
    fun memberFun() {}
}

@Dsl
class Other

context(_: DslReceiver)
fun contextFun() {}

class C

context(_: C)
fun contextFun() {}

fun annotatedFunctionTypeReceiver(f: (@Dsl C).() -> Unit) {}
fun annotatedFunctionType(f: @Dsl (C.() -> Unit)) {}
fun annotatedFunctionTypeWithContext(f: @Dsl (context(C) Other.() -> Unit)) {}

fun <A, R> context(context: A, block: context(A) () -> R): R = block(context)
fun <A, B, R> context(context: A, context2: B, block: context(A, B) () -> R): R = block(context, context2)

val contextFunctionTypeVal: context(DslReceiver) () -> Unit = {}

fun test() {
    with(DslReceiver()) {
        contextFun()
        contextFunctionTypeVal<!NO_VALUE_FOR_PARAMETER!>()<!>

        with(Other()) {
            <!DSL_SCOPE_VIOLATION!>contextFun<!>()
            contextFunctionTypeVal<!NO_VALUE_FOR_PARAMETER!>()<!>
        }
    }

    context(DslReceiver()) {
        contextFun()
        contextFunctionTypeVal<!NO_VALUE_FOR_PARAMETER!>()<!>

        context(Other()) {
            <!DSL_SCOPE_VIOLATION!>contextFun<!>()
            contextFunctionTypeVal<!NO_VALUE_FOR_PARAMETER!>()<!>
        }
    }

    annotatedFunctionTypeReceiver {
        contextFun()

        with(Other()) {
            <!DSL_SCOPE_VIOLATION!>contextFun<!>()
        }
    }

    annotatedFunctionType {
        contextFun()

        with(Other()) {
            <!DSL_SCOPE_VIOLATION!>contextFun<!>()
        }
    }

    annotatedFunctionTypeWithContext {
        // NO DSL_SCOPE_VIOLATION because receiver and context parameter come from the same scope
        <!DSL_SCOPE_VIOLATION!>contextFun<!>()
    }

    context(DslReceiver(), Other()) {
        <!DSL_SCOPE_VIOLATION!>contextFun<!>()
    }
}

context(dsl: DslReceiver)
fun testWithContext() {
    dsl.memberFun()
    with(Other()) {
        dsl.memberFun()
    }
}
