// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-86740
// LANGUAGE_FEATURE_TOGGLED: FixApplicabilityOfEmptyIntersection
fun interface EventHandler<E : Event> {
    fun handle(e: E)
}

open class Event
open class WrongMouseEvent

fun consumeWrongMouseEvent(event: WrongMouseEvent) {}
fun consumeEventHandler(handler: EventHandler<Event>) {}

fun test() {
    consumeEventHandler(<!OTHER_ERROR_WITH_REASON!>EventHandler<!>(::<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>consumeWrongMouseEvent<!>))
}

/* GENERATED_FIR_TAGS: classDeclaration, funInterface, functionDeclaration, interfaceDeclaration, typeConstraint,
typeParameter */
