// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

object Right
object Wrong

interface IA
interface IB : IA
fun IA.foo(vararg x: Int) = Wrong
fun IB.foo(vararg x: Int) = Right
class CC : IB

val test7 = CC().foo(1, 2, 3)

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, integerLiteral,
interfaceDeclaration, objectDeclaration, propertyDeclaration, vararg */
