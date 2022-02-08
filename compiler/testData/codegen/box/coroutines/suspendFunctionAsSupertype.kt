// WITH_STDLIB
// IGNORE_BACKEND: JVM, JS
// !LANGUAGE: +SuspendFunctionAsSupertype

import kotlin.coroutines.*

var failure: String? = "FAIL ILLEGAL STATE"

class TopLevel1: suspend () -> Int {
    override suspend fun invoke(): Int {
        failure = null
        return 42
    }
}

class TopLevel2: suspend (String) -> Int {
    override suspend fun invoke(p: String): Int {
        failure = null
        return p.length
    }
}

class Outer {
    class Nested1: suspend () -> Int {
        override suspend fun invoke(): Int {
            failure = null
            return 42
        }
    }

    class Nested2: suspend (String) -> Int {
        override suspend fun invoke(p: String): Int {
            failure = null
            return p.length
        }
    }
}

interface I : suspend (String) -> Int
abstract class Abstract1 : suspend (String) -> Int
abstract class Abstract2 : suspend (String) -> Int {
    override suspend fun invoke(p: String): Int {
        failure = null
        return p.length
    }
}

fun objectLiteral1(): String? {
    failure = "FAIL OBJECT LITERAL 1"
    val o: suspend () -> Int = object : suspend () -> Int {
        override suspend fun invoke(): Int {
            failure = null
            return 42
        }
    }
    o.startCoroutine(Continuation(EmptyCoroutineContext) { it.getOrThrow() })
    return failure
}

fun objectLiteral2(): String? {
    failure = "FAIL OBJECT LITERAL 2"
    val o: suspend (String) -> Int = object : suspend (String) -> Int {
        override suspend fun invoke(p: String): Int {
            failure = null
            return p.length
        }
    }
    o.startCoroutine("Hello", Continuation(EmptyCoroutineContext) { it.getOrThrow() })
    return failure
}

fun topLevelClass1(): String? {
    failure = "FAIL TOP LEVEL CLASS 1"
    TopLevel1().startCoroutine(Continuation(EmptyCoroutineContext) { it.getOrThrow() })
    return failure
}

fun topLevelClass2(): String? {
    failure = "FAIL TOP LEVEL CLASS 2"
    TopLevel2().startCoroutine("Hello", Continuation(EmptyCoroutineContext) { it.getOrThrow() })
    return failure
}

fun nestedClass1(): String? {
    failure = "FAIL NESTED CLASS 1"
    Outer.Nested1().startCoroutine(Continuation(EmptyCoroutineContext) { it.getOrThrow() })
    return failure
}

fun nestedClass2(): String? {
    failure = "FAIL NESTED CLASS 2"
    Outer.Nested2().startCoroutine("Hello", Continuation(EmptyCoroutineContext) { it.getOrThrow() })
    return failure
}

fun localClass1(): String? {
    failure = "FAIL LOCAL CLASS 1"
    class Local : suspend () -> Int {
        override suspend fun invoke(): Int {
            failure = null
            return 42
        }
    }
    Local().startCoroutine(Continuation(EmptyCoroutineContext) { it.getOrThrow() })
    return failure
}

fun localClass2(): String? {
    fun foo(): String? {
        fun bar(): String? {
            failure = "FAIL LOCAL CLASS 2"
            class Local : suspend (String) -> Int {
                override suspend fun invoke(p: String): Int {
                    failure = null
                    return p.length
                }
            }
            Local().startCoroutine("Hello", Continuation(EmptyCoroutineContext) { it.getOrThrow() })
            return failure
        }
        return bar()
    }
    return foo()
}

fun inherited1(): String? {
    failure = "FAIL INHERITED 1"
    val o = object : I {
        override suspend fun invoke(p: String): Int {
            failure = null
            return p.length
        }
    }
    o.startCoroutine("Hello", Continuation(EmptyCoroutineContext) { it.getOrThrow() })
    return failure
}

fun inherited2(): String? {
    failure = "FAIL INHERITED 2"
    val o = object : I, Abstract1() {
        override suspend fun invoke(p: String): Int {
            failure = null
            return p.length
        }
    }
    o.startCoroutine("Hello", Continuation(EmptyCoroutineContext) { it.getOrThrow() })
    return failure
}

fun inherited3(): String? {
    failure = "FAIL INHERITED 3"
    val o = object : I, Abstract2() {}
    o.startCoroutine("Hello", Continuation(EmptyCoroutineContext) { it.getOrThrow() })
    return failure
}

fun box(): String {
    val failures = listOfNotNull(
        objectLiteral1(),
        objectLiteral2(),
        topLevelClass1(),
        topLevelClass2(),
        nestedClass1(),
        nestedClass2(),
        localClass1(),
        localClass2(),
        inherited1(),
        inherited2(),
        inherited3()
    )

    return if (failures.isNotEmpty()) failures.joinToString("\n") else "OK"
}
