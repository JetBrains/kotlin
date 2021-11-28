// WITH_STDLIB

import kotlin.coroutines.*

interface EntityBase<out ID> {
    suspend fun id(): ID
}

inline class EntityId(val value: String)

interface Entity : EntityBase<EntityId>

var c: Continuation<EntityId>? = null

class EntityStub : Entity {
    override suspend fun id(): EntityId = suspendCoroutine { c = it }
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
    c?.resume(EntityId("OK"))
    return res
}