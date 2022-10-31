// This test checks that we use the `...$lambda-0` method name, instead of `...$lambda$0`, for extracted accessors of local delegated properties.
// It's important to keep these names until we fix the binary compatibility issue KT-49030.

operator fun String.getValue(thisRef: Any?, prop: Any?): String = this
operator fun String.setValue(thisRef: Any?, prop: Any?, value: String) {}

inline fun foo(): String {
    var x by "OK"
    return x
}
