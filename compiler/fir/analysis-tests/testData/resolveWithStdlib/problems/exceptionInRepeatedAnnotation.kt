typealias EventHandler<E> = (e: E) -> Unit

class EventListener<E: Event>() {

    companion object {

        inline operator fun <reified E: Event> invoke(
            noinline callback: EventHandler<E>,
        ): EventListener<E> = TODO()
    }
}

inline fun <reified E: Event> EventHandler<E>.withPriority() = EventListener(this)

inline fun <reified E: Event> EventHandler<E>.withDefaultPriority() = withPriority()

abstract class Event
