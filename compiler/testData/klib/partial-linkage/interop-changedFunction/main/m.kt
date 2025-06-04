import abitestutils.abiTest
import lib.foo

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun box() = abiTest {
    expectFailure(
        linkage(
            "Function 'foo' can not be called: No function found for symbol 'lib/foo|foo(kotlin.Int){}[100]'." +
                    " Looks like a cinterop-generated library issue. It might be that one of the dependencies" +
                    " was compiled with a different version of K/N compiler/cinterop tool than the version used to compile this binary." +
                    " Please check that the project configuration is correct and has consistent versions of all required dependencies." +
                    " It's worth trying to recompile cinterop dependencies with the same K/N version used for compiling this binary." +
                    " See https://youtrack.jetbrains.com/issue/KT-78062 for more details."
        )
    ) {
        foo(42)
    }
}
