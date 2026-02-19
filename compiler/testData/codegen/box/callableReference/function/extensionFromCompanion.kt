class MyClass {
    companion object
}

fun MyClass.Companion.ok() = "OK"

val ref = MyClass.Companion::ok

fun box(): String {
    return ref()
}