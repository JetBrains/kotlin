// No supertype at all

fun Any.extension(arg: Any?) {}

class A1 {
    fun test() {
        super.<!UNRESOLVED_REFERENCE!>extension<!>(null) // Call to an extension function
    }
}
