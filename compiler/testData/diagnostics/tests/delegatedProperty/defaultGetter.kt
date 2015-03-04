// !DIAGNOSTICS: -UNUSED_PARAMETER

val a: Int by Delegate()
    get

class Delegate {
    fun get(t: Any?, p: PropertyMetadata): Int {
        return 1
    }
}