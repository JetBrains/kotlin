// WITH_STDLIB
// ISSUE: KT-69335
// K2 type inference for the below expression from `listOfVarsExport()` has done odd:
// `it(SourceInfoAwareJsNode & JsStatement)` instead of `JsStatement`, or maybe disjunction `SourceInfoAwareJsNode | JsStatement`
//   R|kotlin/collections/listOf|<R|it(SourceInfoAwareJsNode & JsStatement)|>(vararg(R|/JsVars.JsVars|(), R|/JsExport.JsExport|()))
// So, FIR2IR calculated the intersection result as `JsNode`, which is not the element type of vararg: `Array<out JsStatement>`
// Due to it, strict type guards for vararg parameters were relaxed, see `KT-69335` in
//   compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/expressions/impl/builders.kt

// listOfFooBar() also hints on a probable wrong choice to use the intersection:
//   R|kotlin/collections/listOf|<R|it(SourceInfoAwareJsNode & SomeOtherInterface)|>(vararg(R|/Foo.Foo|(), R|/Bar.Bar|()))
// later, `it(SourceInfoAwareJsNode & SomeOtherInterface)` is calculated as Any, while K1 infers `SomeOtherInterface`

interface JsNode
interface JsStatement : JsNode
abstract class SourceInfoAwareJsNode : JsNode
class JsExport: SourceInfoAwareJsNode(), JsStatement
class JsVars: SourceInfoAwareJsNode(), JsStatement
fun listOfVarsExport(): List<JsStatement> = listOf(JsVars(), JsExport())

abstract class NonJsNode
class JsNonJsNode : NonJsNode(), JsStatement
fun listOfVarsNonJsNode(): List<JsStatement> = listOf(JsVars(), JsNonJsNode())

interface SomeOtherInterface
class Foo: SourceInfoAwareJsNode(), SomeOtherInterface
class Bar: SourceInfoAwareJsNode(), SomeOtherInterface
fun listOfFooBar(): List<SomeOtherInterface> = listOf(Foo(), Bar())

open class Granny
open class Parent: Granny()
class Child1: Parent()
class Child2: Parent()
fun listOfChild1Child2(): List<Granny> = listOf(Child1(), Child2())
