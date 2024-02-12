// ISSUE: KT-65584
// WITH_STDLIB

fun interface Flow<out T> {

    suspend fun collect(collector: FlowCollector<T>)
}

fun interface FlowCollector<in T> {

    suspend fun emit(value: T)
}

inline fun <T, R> Flow<T>.flatMapLatest(crossinline transform: suspend (value: T) -> Flow<R>) = Flow { collector ->
    collect { it1 -> transform(it1).collect { it2 -> collector.emit(it2) } }
}

fun <T> flowOf(value: T): Flow<T> = Flow { collector -> collector.emit(value) }

inline fun <T, R> Flow<T>.map(crossinline transform: suspend (value: T) -> R): Flow<R> = Flow { collector ->
    collect { collector.emit(transform(it)) }
}

// ------

class StationId
class Playable
class Entity(val stationId: StationId)
class State(val playbackEntity: Entity)

internal suspend fun init(
    queueState: State,
    state: Flow<Playable>
) {
    state
        .flatMapLatest <!REPEATED_ANNOTATION!>{ playable ->
            flowOf(playable).map <!REPEATED_ANNOTATION!>{ Triple(it, playable, queueState.playbackEntity.stationId) }<!>
        }<!>
        .collect { <!REPEATED_ANNOTATION!>(<!REPEATED_ANNOTATION!>likeState<!>, playable, stationId)<!> ->
        }
}
