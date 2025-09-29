// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-77301
// LANGUAGE: +ContextParameters
@DslMarker
annotation <!DSL_MARKER_WITH_DEFAULT_TARGETS!>class MyMarker<!>

@MyMarker
class DslReceiver

@MyMarker
class DslReceiver2

class SomeOtherClass

fun foo1(block: context(SomeOtherClass) DslReceiver.() -> Unit) {}
fun foo2(block: context(DslReceiver) SomeOtherClass.() -> Unit) {}
fun foo3(block: context(SomeOtherClass, DslReceiver) () -> Unit) {}
fun foo4(block: context(DslReceiver, SomeOtherClass) () -> Unit) {}

context(c: SomeOtherClass)
val nonDslVal: SomeOtherClass
    get() = c

context(c: DslReceiver)
val dslVal: DslReceiver
    get() = c

fun test() {
    foo1 {
        nonDslVal
        dslVal
        with(DslReceiver2()) {
            nonDslVal
            <!DSL_SCOPE_VIOLATION!>dslVal<!>
        }
    }
    foo2 {
        nonDslVal
        dslVal
        with(DslReceiver2()) {
            nonDslVal
            <!DSL_SCOPE_VIOLATION!>dslVal<!>
        }
    }
    foo3 {
        nonDslVal
        dslVal
        with(DslReceiver2()) {
            nonDslVal
            <!DSL_SCOPE_VIOLATION!>dslVal<!>
        }
    }
    foo4 {
        nonDslVal
        dslVal
        with(DslReceiver2()) {
            nonDslVal
            <!DSL_SCOPE_VIOLATION!>dslVal<!>
        }
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, functionalType, getter,
lambdaLiteral, propertyDeclaration, propertyDeclarationWithContext, typeWithContext, typeWithExtension */
