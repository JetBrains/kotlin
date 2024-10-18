import kotlin.reflect.KProperty

class DelegateExtension {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String.() -> String {
        return { this }
    }
}

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): (String) -> String {
        return { a: String -> a }
    }
}

val String.o: String.() -> String by DelegateExtension()
val String.k: String.() -> String by Delegate()

fun box(): String {
    return "FAIL".o("O") + "FAIL".k("K")
}