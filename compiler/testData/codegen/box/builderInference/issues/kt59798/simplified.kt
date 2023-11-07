// ISSUE: KT-59798

class Buildee<T1, T2> {
    fun put(key: T1, value: T2) {}
    fun remove(key: T1): T2 = TargetTypeV() as T2
}

fun <T1, T2> build(block: Buildee<T1, T2>.() -> Unit): Buildee<T1, T2> {
    return Buildee<T1, T2>().apply(block)
}

class TargetTypeK
class TargetTypeV

fun box(): String {
    build {
        put(TargetTypeK(), TargetTypeV())
        remove(TargetTypeK())
    }
    build {
        put(TargetTypeK(), TargetTypeV())
        remove(TargetTypeK()).let {} // K1: BUILDER_INFERENCE_STUB_RECEIVER
    }
    return "OK"
}
