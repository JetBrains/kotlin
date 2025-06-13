// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
// WITH_STDLIB
// ISSUE: KT-58534, KT-68687

// FILE: test1.kt
package test1

private typealias T = context(Int, Long) () -> Unit

fun a(a: T) = Unit

val b = a {
    foo()
}

context(_: Int, _: Long)
fun foo() {}

// FILE: test2.kt
package test2

interface ConfigScope
interface ConfigError

typealias Validated<A> = Either<ConfigError, A>

sealed class Either<out E, out A> {
    data class Left<out E>(val error: E) : Either<E, Nothing>()
    data class Right<out A>(val value: A) : Either<Nothing, A>()
}

fun <A> A.right() = Either.Right(this)

object ConfigurationContext
object CreationContext

typealias Configuration<C> = context(ConfigurationContext) ConfigScope.() -> Validated<C>
typealias Creation<C, T> = context(CreationContext) (C) -> Validated<T>

class Definition<C, T>(
    val name: String,
    val configuration: Configuration<C>,
    val creation: Creation<C, T>,
)

class Source(val n: Int)

fun sources(block: BuilderDsl<Source>.() -> Unit) = BuilderDsl<Source>().apply(block).things

class BuilderDsl<T> {
    val things = mutableMapOf<String, Definition<*, T>>()

    infix fun <C> String.configuredBy(config: Configuration<C>) =
        Pair(this, config)

    infix fun <C> Pair<String, Configuration<C>>.createdBy(creator: Creation<C, T>) {
        Definition(first, second, creator)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, data, funWithExtensionReceiver, functionDeclaration,
functionDeclarationWithContext, functionalType, infix, interfaceDeclaration, lambdaLiteral, nestedClass, nullableType,
objectDeclaration, out, primaryConstructor, propertyDeclaration, sealed, starProjection, thisExpression,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter, typeWithContext, typeWithExtension */
