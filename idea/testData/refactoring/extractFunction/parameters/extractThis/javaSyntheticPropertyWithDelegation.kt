// WITH_RUNTIME
// PARAM_DESCRIPTOR: private final fun NamedEx.foo(): kotlin.String defined in Test
// PARAM_TYPES: NamedEx
// SIBLING:
public class Test {
    private fun NamedEx.foo() = <selection>name</selection>
}

public class NamedEx : Named by object : Named {
    override fun getName(): String = "foo"
}