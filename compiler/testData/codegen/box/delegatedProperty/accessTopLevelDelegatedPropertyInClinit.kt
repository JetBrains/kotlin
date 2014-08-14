// KT-5612

class Delegate {
    public fun get(thisRef: Any?, prop: PropertyMetadata): String {
        return "OK"
    }
}

val prop by Delegate()

val a = prop

fun box() = a
