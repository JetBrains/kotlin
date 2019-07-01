// IGNORE_BACKEND: JVM_IR
public fun invokeCoroutineBuilder() {
    return buildCoroutine {
    }
}

public fun buildCoroutine(builderAction: suspend Any.() -> Unit) {

}