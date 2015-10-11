// !DIAGNOSTICS: -UNUSED_PARAMETER

var a: Int by Delegate()
    private set

class Delegate {
    fun getValue(t: Any?, p: PropertyMetadata): Int {
        return 1
    }

    fun setValue(t: Any?, p: PropertyMetadata, i: Int) {}
}