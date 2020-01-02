// No supertype at all

fun Any.extension(arg: Any?) {}

class A1 {
    fun test() {
        super.extension(null) // Call to an extension function
    }
}
