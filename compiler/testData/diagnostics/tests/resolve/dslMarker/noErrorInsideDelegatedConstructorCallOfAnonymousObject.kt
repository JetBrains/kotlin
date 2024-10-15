// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-71710

@DslMarker
annotation class NoriaDsl

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
