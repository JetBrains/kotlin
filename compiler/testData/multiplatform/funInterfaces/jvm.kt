// ADDITIONAL_COMPILER_ARGUMENTS: -XXLanguage:+NewInference
// ADDITIONAL_COMPILER_ARGUMENTS: -XXLanguage:+SamConversionForKotlinFunctions
// ADDITIONAL_COMPILER_ARGUMENTS: -XXLanguage:+SamConversionPerArgument

package jvm

fun interface KRunnable {
    fun invoke(): String
}

fun foo(k: KRunnable) = k.invoke()

fun test() {
    foo { "OK" }
}