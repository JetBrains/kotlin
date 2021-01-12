
// KOTLIN_SCRIPT_DEFINITION: org.jetbrains.kotlin.codegen.TestScriptWithSimpleEnvVars

// envVar: stringVar1=abracadabra

fun foo(body: () -> String): String = body()

val res = foo { stringVar1.drop(5) }

// expected: res=adabra
