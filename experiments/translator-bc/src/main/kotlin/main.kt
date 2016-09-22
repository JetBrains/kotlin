import com.intellij.openapi.util.Disposer
import com.jshmrsn.karg.parseArguments
import org.kotlinnative.translator.ProjectTranslator
import org.kotlinnative.translator.TranslationState
import java.io.*

fun main(args: Array<String>) {
    // TODO make good help message (ex. is rm --help)
    val arguments = parseArguments(args, ::DefaultArguments)
    val disposer = Disposer.newDisposable()
    val analyzedFiles = arguments.sources.toMutableList()

    if (arguments.includeDir != null) {
        val libraryFiles = File(arguments.includeDir).walk().filter { !it.isDirectory }.map { it.absolutePath }
        analyzedFiles.addAll(libraryFiles)
    }

    val translationState = TranslationState.createTranslationState(analyzedFiles, disposer, arguments.mainClass, arguments.arm)
    val code:String = ProjectTranslator(
            translationState.environment.getSourceFiles(),
            translationState
    ).generateCode()

    if (arguments.output == null) {
        println(code)
    } else {
        val output = File(arguments.output)
        output.writeText(code)
    }
}