// No supertype at all

fun Any.extension(<!UNUSED_PARAMETER!>arg<!>: Any?) {}

class A1 {
    fun test() {
        <!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.extension(null) // Call to an extension function
    }
}
