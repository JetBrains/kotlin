// ISSUE: KT-47989

class Buildee<T> {
    fun materialize(): T = TargetType() as T
}

fun <T> build(block: Buildee<T>.() -> Unit): Buildee<T> {
    return Buildee<T>().apply(block)
}

class TargetType
interface TypeSourceInterfaceA {
    fun typeSourceMember(): TargetType
}
interface TypeSourceInterfaceB {
    fun typeSourceMember(): Buildee<TargetType>
}

fun box(): String {
    // K1&K2: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER
    build {
        object: TypeSourceInterfaceA {
            override fun typeSourceMember() = materialize()
        }
    }
    // K1&K2: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER
    build {
        object: TypeSourceInterfaceB {
            override fun typeSourceMember() = this@build
        }
    }
    return "OK"
}
