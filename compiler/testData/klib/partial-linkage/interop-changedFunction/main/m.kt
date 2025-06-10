import abitestutils.abiTest
import lib.foo

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun box() = abiTest {
    expectFailure(
        linkage(
            "Function 'foo' can not be called: No function found for symbol 'lib/foo|foo(kotlin.Int){}[100]'." +
                    " This looks like a cinterop-generated library issue. It could happen if there is a cinterop dependency" +
                    " generated with a different version of the Kotlin/Native compiler than the version used to compile this binary." +
                    " Please check that the project configuration is correct and has consistent versions of all required dependencies." +
                    " It's worth trying to regenerate cinterop dependencies with the same version of" +
                    " the Kotlin/Native compiler that was used for compiling this binary." +
                    " See https://youtrack.jetbrains.com/issue/KT-78062 for more details."
        )
    ) {
        foo(42)
    }
}
