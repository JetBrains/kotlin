// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-86740
// LANGUAGE: +ForbidInferringTypeVariablesIntoEmptyIntersection
// LANGUAGE_FEATURE_TOGGLED: FixApplicabilityOfEmptyIntersection
fun interface EventHandler<E : Event> {
    fun handle(e: E)
}

interface Event
class WrongMouseEvent

fun consumeWrongMouseEvent(event: WrongMouseEvent) {}
fun consumeEventHandler(handler: EventHandler<Event>) {}

fun test() {
    consumeEventHandler(<!ARGUMENT_TYPE_MISMATCH!>EventHandler(::consumeWrongMouseEvent)<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, funInterface, functionDeclaration, interfaceDeclaration, typeConstraint,
typeParameter */
