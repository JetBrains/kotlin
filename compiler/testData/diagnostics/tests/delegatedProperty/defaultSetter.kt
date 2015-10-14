// !DIAGNOSTICS: -UNUSED_PARAMETER

var a: Int by Delegate()
    private set

class Delegate {
    operator fun getValue(t: Any?, p: PropertyMetadata): Int {
        return 1
    }

    operator fun setValue(t: Any?, p: PropertyMetadata, i: Int) {}
}