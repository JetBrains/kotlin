import org.jetbrains.kotlin.arguments.CompilerArgumentsTopLevel
import org.jetbrains.kotlin.arguments.CompilerArgumentsTopLevelBuilder

@DslMarker
annotation class KotlinArgumentsDslMarker

@KotlinArgumentsDslMarker
fun compilerArguments(
    topLevelName: String,
    config: CompilerArgumentsTopLevelBuilder.() -> Unit,
): CompilerArgumentsTopLevel {
    val kotlinArguments = CompilerArgumentsTopLevelBuilder(topLevelName)
    config(kotlinArguments)
    return kotlinArguments.build()
}
