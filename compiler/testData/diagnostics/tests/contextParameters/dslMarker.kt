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

<!CONFLICTING_OVERLOADS!><!CONTEXT_PARAMETERS_UNSUPPORTED!>context(_: <!DEBUG_INFO_MISSING_UNRESOLVED!>DslReceiver<!>)<!>
fun contextFun()<!> {}

class C

<!CONFLICTING_OVERLOADS!><!CONTEXT_PARAMETERS_UNSUPPORTED!>context(_: <!DEBUG_INFO_MISSING_UNRESOLVED!>C<!>)<!>
fun contextFun()<!> {}

fun annotatedFunctionTypeReceiver(f: (@Dsl C).() -> Unit) {}
fun annotatedFunctionType(f: @Dsl (C.() -> Unit)) {}
fun annotatedFunctionTypeWithContext(f: @Dsl (context(C) Other.() -> Unit)) {}

fun <A, R> context(context: A, block: context(A) () -> R): R = block(context)
fun <A, B, R> context(context: A, context2: B, block: <!SUBTYPING_BETWEEN_CONTEXT_RECEIVERS!>context(A, B)<!> () -> R): R = block(context, context2)

val contextFunctionTypeVal: context(DslReceiver) () -> Unit = {}

fun test() {
    with(DslReceiver()) {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>contextFun<!>()
        contextFunctionTypeVal<!NO_VALUE_FOR_PARAMETER!>()<!>

        with(Other()) {
            <!OVERLOAD_RESOLUTION_AMBIGUITY!>contextFun<!>()
            contextFunctionTypeVal<!NO_VALUE_FOR_PARAMETER!>()<!>
        }
    }

    context(DslReceiver()) {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>contextFun<!>()
        contextFunctionTypeVal<!NO_VALUE_FOR_PARAMETER!>()<!>

        context(Other()) {
            <!OVERLOAD_RESOLUTION_AMBIGUITY!>contextFun<!>()
            contextFunctionTypeVal<!NO_VALUE_FOR_PARAMETER!>()<!>
        }
    }

    annotatedFunctionTypeReceiver {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>contextFun<!>()

        with(Other()) {
            <!OVERLOAD_RESOLUTION_AMBIGUITY!>contextFun<!>()
        }
    }

    annotatedFunctionType {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>contextFun<!>()

        with(Other()) {
            <!OVERLOAD_RESOLUTION_AMBIGUITY!>contextFun<!>()
        }
    }

    annotatedFunctionTypeWithContext {
        // NO DSL_SCOPE_VIOLATION because receiver and context parameter come from the same scope
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>contextFun<!>()
    }

    context(DslReceiver(), Other()) {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>contextFun<!>()
    }
}

<!CONTEXT_PARAMETERS_UNSUPPORTED!>context(dsl: <!DEBUG_INFO_MISSING_UNRESOLVED!>DslReceiver<!>)<!>
fun testWithContext() {
    <!UNRESOLVED_REFERENCE!>dsl<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>memberFun<!>()
    with(Other()) {
        <!UNRESOLVED_REFERENCE!>dsl<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>memberFun<!>()
    }
}
