// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-71710

@DslMarker
annotation <!DSL_MARKER_WITH_DEFAULT_TARGETS!>class NoriaDsl<!>

class NoriaState

abstract class NoriaContext(val noriaState: NoriaState?)

@NoriaDsl
abstract class ThemedContext(state: NoriaState?) : NoriaContext(state)

abstract class AbsoluteContext(noria: NoriaState?) : ThemedContext(noria)

class Context(noria: NoriaState?) : AbsoluteContext(noria)

fun ThemedContext.absolute() {
    object : AbsoluteContext(noriaState) {}

    class LocalClass : AbsoluteContext(noriaState)
}

/* GENERATED_FIR_TAGS: annotationDeclaration, anonymousObjectExpression, classDeclaration, funWithExtensionReceiver,
functionDeclaration, localClass, nullableType, primaryConstructor, propertyDeclaration */
