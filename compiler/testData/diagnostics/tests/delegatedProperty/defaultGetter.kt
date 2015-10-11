// !DIAGNOSTICS: -UNUSED_PARAMETER

val a: Int by Delegate()
    get

class Delegate {
    operator fun getValue(t: Any?, p: PropertyMetadata): Int {
        return 1
    }
}