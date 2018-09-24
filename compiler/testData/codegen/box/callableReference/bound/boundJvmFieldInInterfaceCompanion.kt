// !LANGUAGE: +JvmFieldInInterface
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JS
// IGNORE_BACKEND: JS_IR
// WITH_RUNTIME

class Bar(val value: String)

interface  Foo {

    companion object {
        @JvmField
        val z = Bar("OK")
    }
}


fun box(): String {
    val field = Foo.Companion::z
    return field.get().value
}
