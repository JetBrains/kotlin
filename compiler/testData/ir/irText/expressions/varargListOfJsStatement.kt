// WITH_STDLIB

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