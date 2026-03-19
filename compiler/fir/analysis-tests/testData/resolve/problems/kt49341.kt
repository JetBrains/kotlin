// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-49341
// WITH_STDLIB

// KT-49341: Kotlin Gradle DSL script compilation crashes with NoSuchElementException: List is empty
// Passing Class instead of KClass to an overloaded function with a lambda should give an error, not crash.

interface NamedDomainObjectContainer<T>
interface PolymorphicDomainObjectContainer<T> : NamedDomainObjectContainer<T>

fun <T : Any, C : NamedDomainObjectContainer<T>> C.registering(action: T.() -> Unit): String = ""
fun <T : Any, C : PolymorphicDomainObjectContainer<T>, U : T> C.registering(type: kotlin.reflect.KClass<U>): Int = 0

abstract class MyTask

class TaskContainer : PolymorphicDomainObjectContainer<MyTask>

fun test(tasks: TaskContainer) {
    tasks.<!NONE_APPLICABLE!>registering<!>(MyTask::class.java) {}
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, funWithExtensionReceiver, functionDeclaration, functionalType,
integerLiteral, interfaceDeclaration, lambdaLiteral, nullableType, stringLiteral, typeConstraint, typeParameter,
typeWithExtension */
