// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
// DIAGNOSTICS: -UNUSED_PARAMETER -SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE
// ISSUE: KT-77836

@kotlin.coroutines.RestrictsSuspension
sealed interface Region<R>

private class RegionImpl<R> : Region<R>

fun <R> region(block: suspend Region<*>.() -> R): R = TODO()
suspend fun <R, T> Region<R>.subregion(block: suspend Region<out R>.() -> T): T = block(RegionImpl<R>())

class MyPath(val path: String)
class MyFileHandle<R>

context(region: Region<out R>)
suspend fun <R> MyPath.open(): MyFileHandle<R> = MyFileHandle()

context(region: Region<out R>)
suspend fun <R> MyFileHandle<R>.read(): Char = 'a'

suspend fun <R> Region<R>.example() {
    val file = MyPath("example.txt").open()
    println(file.read())
    val file2 = subregion {
        suspend fun <R1 : R> Region<R1>.subExample(): MyFileHandle<R> {
            val subfile = MyPath("example2.txt").open()
            println(file.read())
            println(subfile.read())
            return MyPath("example3.txt").open<R>()
        }
        subExample()
    }
    file2.read()
}

fun main() = region {
    example()
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext,
functionalType, interfaceDeclaration, lambdaLiteral, localFunction, localProperty, nullableType, outProjection,
primaryConstructor, propertyDeclaration, sealed, starProjection, stringLiteral, suspend, typeConstraint, typeParameter,
typeWithExtension */
