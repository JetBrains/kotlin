// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

<!ILLEGAL_JVM_NAME!>@JvmName("with.dot")<!>
fun foo1() {}

<!ILLEGAL_JVM_NAME!>@JvmName("with;semicolon")<!>
fun foo2() {}

<!ILLEGAL_JVM_NAME!>@JvmName("with[open square")<!>
fun foo3() {}

<!ILLEGAL_JVM_NAME!>@JvmName("with/slash")<!>
fun foo4() {}

/* GENERATED_FIR_TAGS: functionDeclaration, stringLiteral */
