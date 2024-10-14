interface EventResult

interface EventData<ER : EventResult>

class SomeEvent<ED : EventData<*>>(val eventData: ED)

fun processEvent(event: SomeEvent<*>) {
    <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>processEventData<!>(<!ARGUMENT_TYPE_MISMATCH!>event.eventData<!>)
}

fun <ER : EventResult, ED : EventData<ER>> processEventData(eventData: ED) {}
