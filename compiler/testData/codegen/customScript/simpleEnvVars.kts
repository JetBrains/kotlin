
// KOTLIN_SCRIPT_DEFINITION: org.jetbrains.kotlin.test.runners.codegen.TestScriptWithSimpleEnvVars

// envVar: stringVar1=abracadabra

val res = stringVar1.drop(4)

// expected: res=cadabra
