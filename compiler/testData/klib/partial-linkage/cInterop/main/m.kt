import abitestutils.abiTest
import lib.foo

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun box() = abiTest {
    expectSuccess("foo") { ::foo.name }
    expectFailure(
        linkage(
            "Reference to function 'foo' can not be evaluated: No function found for symbol 'lib/foo'." +
                    " This looks like a cinterop-generated library issue. It could happen if there is a transitive dependency which" +
                    " uses cinterop and the resulting libraries are not binary compatible. Or there might be a cinterop dependency" +
                    " generated with a different version of the Kotlin/Native compiler than the version used to compile this binary." +
                    " Please check that the project configuration is correct and has consistent versions of all required dependencies." +
                    " See https://youtrack.jetbrains.com/issue/KT-78062 for more details."
        )
    ) { ::foo.toString() }
    expectFailure(
        linkage(
            "Function 'foo' can not be called: No function found for symbol 'lib/foo'." +
                    " This looks like a cinterop-generated library issue. It could happen if there is a transitive dependency which" +
                    " uses cinterop and the resulting libraries are not binary compatible. Or there might be a cinterop dependency" +
                    " generated with a different version of the Kotlin/Native compiler than the version used to compile this binary." +
                    " Please check that the project configuration is correct and has consistent versions of all required dependencies." +
                    " See https://youtrack.jetbrains.com/issue/KT-78062 for more details."
        )
    ) { foo(42) }
}
