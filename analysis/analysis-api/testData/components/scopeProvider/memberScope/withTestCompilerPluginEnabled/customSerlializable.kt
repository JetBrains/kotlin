// WITH_FIR_TEST_COMPILER_PLUGIN
package test

@org.jetbrains.kotlin.plugin.sandbox.MySerializable
class FirstTarget

@org.jetbrains.kotlin.plugin.sandbox.MySerializable
class SecondTarget

@org.jetbrains.kotlin.plugin.sandbox.CoreSerializer
class Serializer

// class: test/Serializer
