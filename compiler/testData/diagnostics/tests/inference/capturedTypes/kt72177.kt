// FIR_IDENTICAL
interface EventResult

interface EventData<ER : EventResult>

class SomeEvent<ED : EventData<*>>(val eventData: ED)

fun processEvent(event: SomeEvent<*>) {
    processEventData(event.eventData)
}

fun <ER : EventResult, ED : EventData<ER>> processEventData(eventData: ED) {}
