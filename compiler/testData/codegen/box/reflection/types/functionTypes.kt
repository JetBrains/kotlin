// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class A {
    fun function0(): (() -> Unit) = null!!
    fun function1(): ((String) -> Unit)? = null
    fun function3(): ((Any, Int, Unit?) -> LongArray)? = null
    fun suspendFunction0(): (suspend () -> Unit)? = null
    fun suspendFunction1(): (suspend (String) -> Unit) = null!!
    fun suspendFunction3(): (suspend (Any, Int, Unit?) -> LongArray)? = null
}

fun check(function: KCallable<*>, typeToString: String, classFqName: String, arguments: String) {
    val type = function.returnType
    assertEquals(typeToString.endsWith("?"), type.isMarkedNullable, "Fail: isMarkedNullable for $type")
    val classifier = type.classifier as? KClass<*> ?: error("Fail: no classifier for $type")
    assertEquals(classFqName, classifier.qualifiedName, "Fail: qualified name for $type")
    assertEquals(arguments, type.arguments.toString(), "Fail: arguments for $type")
    assertEquals(typeToString, type.toString())
}

fun box(): String {
    check(A::function0, "() -> kotlin.Unit", "kotlin.Function0", "[kotlin.Unit]")
    check(A::function1, "((kotlin.String) -> kotlin.Unit)?", "kotlin.Function1", "[kotlin.String, kotlin.Unit]")
    check(A::function3, "((kotlin.Any, kotlin.Int, kotlin.Unit?) -> kotlin.LongArray)?", "kotlin.Function3", "[kotlin.Any, kotlin.Int, kotlin.Unit?, kotlin.LongArray]")
    // After KT-79225, classifier should be "kotlin.coroutines.SuspendFunction{arity}"
    check(A::suspendFunction0, "(suspend () -> kotlin.Unit)?", "kotlin.Function1", "[kotlin.Unit]")
    check(A::suspendFunction1, "suspend (kotlin.String) -> kotlin.Unit", "kotlin.Function2", "[kotlin.String, kotlin.Unit]")
    check(A::suspendFunction3, "(suspend (kotlin.Any, kotlin.Int, kotlin.Unit?) -> kotlin.LongArray)?", "kotlin.Function4", "[kotlin.Any, kotlin.Int, kotlin.Unit?, kotlin.LongArray]")

    return "OK"
}
