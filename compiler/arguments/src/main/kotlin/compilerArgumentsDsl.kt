import org.jetbrains.kotlin.arguments.CompilerArguments
import org.jetbrains.kotlin.arguments.CompilerArgumentsBuilder

@DslMarker
annotation class KotlinArgumentsDslMarker

@KotlinArgumentsDslMarker
fun compilerArguments(
    config: CompilerArgumentsBuilder.() -> Unit,
): CompilerArguments {
    val kotlinArguments = CompilerArgumentsBuilder()
    config(kotlinArguments)
    return kotlinArguments.build()
}
