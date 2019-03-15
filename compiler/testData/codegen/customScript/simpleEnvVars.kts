
// KOTLIN_SCRIPT_DEFINITION: org.jetbrains.kotlin.codegen.TestScriptWithSimpleEnvVars

// envVar: stringVar1=abracadabra

val res = stringVar1.drop(4)

// expected: res=cadabra
