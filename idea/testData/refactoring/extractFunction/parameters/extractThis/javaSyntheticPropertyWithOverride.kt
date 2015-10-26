// WITH_RUNTIME
// PARAM_DESCRIPTOR: private final fun NamedEx.foo(): [@org.jetbrains.annotations.NotNull] String defined in Test
// PARAM_TYPES: NamedEx
// SIBLING:
public class Test {
    private fun NamedEx.foo() = <selection>name</selection>
}