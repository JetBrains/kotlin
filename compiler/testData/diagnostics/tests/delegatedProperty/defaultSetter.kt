// !DIAGNOSTICS: -UNUSED_PARAMETER

var a: Int by Delegate()
    private set

class Delegate {
    fun get(t: Any?, p: PropertyMetadata): Int {
        return 1
    }

    fun set(t: Any?, p: PropertyMetadata, i: Int) {}
}