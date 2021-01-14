package noReflect

fun main() {
    try {
        String::class.annotations
    } catch (e: KotlinReflectionNotSupportedError) {
        println("KotlinReflectionNotSupportedError has been caught")
    }
}