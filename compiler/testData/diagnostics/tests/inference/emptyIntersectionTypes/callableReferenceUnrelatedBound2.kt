// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-86740
// LANGUAGE: +ForbidInferringTypeVariablesIntoEmptyIntersection
fun interface EventHandler<E : Event> {
    fun handle(e: E)
}

interface Event
class WrongMouseEvent

fun consumeWrongMouseEvent(event: WrongMouseEvent) {}
fun consumeEventHandler(handler: EventHandler<Event>) {}

fun test() {
    consumeEventHandler(<!OTHER_ERROR_WITH_REASON!>EventHandler<!>(::<!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION, OTHER_ERROR_WITH_REASON!>consumeWrongMouseEvent<!>))
}

/* GENERATED_FIR_TAGS: classDeclaration, funInterface, functionDeclaration, interfaceDeclaration, typeConstraint,
typeParameter */
