// WITH_FIR_TEST_COMPILER_PLUGIN
package test

@org.jetbrains.kotlin.fir.plugin.MySerializable
class FirstTarget

@org.jetbrains.kotlin.fir.plugin.MySerializable
class SecondTarget

@org.jetbrains.kotlin.fir.plugin.CoreSerializer
class Serializer

fun test(serializer: Serializer, target: FirstTarget) {
    serializer.<expr>serializeFirstTarget</expr>(target)
}