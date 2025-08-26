// TARGET_BACKEND: JVM_IR, WASM
// IGNORE_BACKEND_K1: JVM_IR, WASM
// ISSUE: KT-54931

class Container {
    annotation class ExampleMapKey(
        val stringValue: String,
    )
}

object Container_ExampleMapKeyCreator {
    fun createExampleMapKey(stringValue: String) = Container.ExampleMapKey(stringValue) // Error is here
}

fun box() = Container_ExampleMapKeyCreator.createExampleMapKey("OK").stringValue
