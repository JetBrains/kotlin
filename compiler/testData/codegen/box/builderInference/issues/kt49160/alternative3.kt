// ISSUE: KT-49160

class Buildee<T> {
    fun yield(arg: T) {}
}

fun <T> build(block: Buildee<T>.() -> Unit): Buildee<T> {
    return Buildee<T>().apply(block)
}

class TargetType

fun box(): String {
    // K1&K2: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER
    build outerBuild@ {
        object {
            fun anonymousObjectMember() {
                build innerBuild@ {
                    this@outerBuild.yield(TargetType())
                    this@innerBuild.yield(TargetType())
                }
            }
        }
    }
    return "OK"
}