// WITH_RUNTIME
public fun invokeCoroutineBuilder() {
    return buildCoroutine {
        println(this)
    }
}

public fun buildCoroutine(builderAction: suspend Any.() -> Unit) {

}