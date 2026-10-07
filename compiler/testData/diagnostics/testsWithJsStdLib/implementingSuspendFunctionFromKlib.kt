// RUN_PIPELINE_TILL: CODEGEN

// MODULE: lib
// FILE: lib.kt
package library

@Suppress("IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE")
interface Work<T> : suspend (T) -> T

// MODULE: main(lib)
// FILE: main.kt
package consumer

import library.Work

abstract class <!IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>AbstractWork<!> : Work<String>

class <!IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>ConcreteWork<!> : Work<String> {
    override suspend fun invoke(value: String): String = value
}
