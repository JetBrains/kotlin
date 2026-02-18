// NO_CHECK_LAMBDA_INLINING
// FILE: lib.kt
inline fun <
        reified TService : Service<TService, TEvent>,
        reified TEvent : Event<TService>> event(
    noinline handler: suspend (TEvent) -> Unit
) {
    val serviceKlass = TService::class
    val eventKlass = TEvent::class
}

interface Service<
        Self : Service<Self, TEvent>,
        in TEvent : Event<Self>
        >

interface Event<out T : Service<out T, *>>

// FILE: main.kt
class SomeService : Service<SomeService, SomeService.SomeEvent> {
    class SomeEvent : Event<SomeService>
}

fun box(): String {
    event { someEvent: SomeService.SomeEvent ->  } // REIFIED_TYPE_FORBIDDEN_SUBSTITUTION
    return "OK"
}
