// WITH_STDLIB
// IGNORE_BACKEND: JVM

import kotlin.coroutines.*

interface EntityBase<out ID> {
    suspend fun id(): ID
}

inline class EntityId(val value: String)

interface Entity : EntityBase<EntityId>

class EntityStub : Entity {
    override suspend fun id(): EntityId = EntityId("OK")
}

suspend fun test(): EntityId {
    val entity: Entity = EntityStub()
    return entity.id()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun box(): String {
    var res = "FAIL"
    builder {
        res = test().value
    }
    return res
}