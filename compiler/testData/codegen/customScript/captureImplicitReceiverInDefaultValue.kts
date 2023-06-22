// TARGET_BACKEND: JVM_IR
// KOTLIN_SCRIPT_DEFINITION: org.jetbrains.kotlin.codegen.TestScriptWithReceivers

// receiver: abracadabra
// expected: rv=cadabra

// KT-55068

class User(var property: String = drop(4))

val rv = User().property
