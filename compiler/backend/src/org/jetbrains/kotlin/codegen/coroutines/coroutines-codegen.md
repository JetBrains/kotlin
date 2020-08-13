# Coroutines Codegen

This document aims to collect every piece of information about coroutines codegen in one place, so, instead of reading the compiler code or
writing snippets and looking at resulting bytecode, a programmer can check the document and find a section which explains how and, more 
importantly, why the compiler behaves like this (or, to be precise, should behave like this). Hopefully, this will help people working on 
the compiler and advanced Kotlin programmers to understand the reasons behind specific design decisions.

The document is JVM-centric, that means it explains how things work in JVM BE since this is the area I am most familiar with and since in 
JVM, there are guaranties of backward compatibility, which the compiler shall obey in both so-called "Old JVM" back-end, as well as in the 
new JVM_IR one. The naming of the new back-end can differ from the official documentation: the document uses the "IR" suffix, while the 
official documentation omits it.

If the name of a section of the document has an "Old JVM:" prefix, it explains old JVM back-end specific details; if the prefix is "JVM_IR," 
then it is JVM_IR back-end specific. If the prefix is plain "JVM," the explanation applies to both the old back-end and the new one. If there 
is no such prefix, the section explains the general behavior of coroutines and shall apply to all back-ends.

The document sticks to release coroutines since we deprecated experimental coroutines in 1.3, and JVM_IR does
not support them. However, there are sections, which explain differences in code generation between release and experimental coroutines 
wherever appropriate, since we technically still support them. Sections, which describe experimental coroutines, have a "1.2" prefix.

If the current implementation is not ideal (or has a bug), there is a description of the difference and the steps to implement the "correct" 
version. These subsections start with "FIXME."

Throughout the document term "coroutine" will represent either a suspend lambda or a suspend function, which is different from the usual 
definition of coroutines - something like a lightweight thread. The document reuses the term since "suspend lambda or function" is wordy, 
and when it requires the typical definition, it says explicitly "a coroutine in a broad sense."

The document often uses the term "undefined behavior," which means that we consciously rejected defining the behavior. Thus, the behavior 
may vary from version to version, from back-end to back-end, and one should use it with extreme caution.

Lastly, most of the examples presented in the document actually suspend, so one is sure every piece is in place since coroutines is a broad
and complex topic, and it is easy to forget one piece, which will lead to a runtime error or even worse, semantically wrong code execution.

## Suspend Lambda
Let us begin by introducing suspend lambdas.

Suspend lambda is an example of a coroutine, and the compiler turns ordinary, sequential code inside the lambda into suspendable.
The example shows a simple suspend lambda:
```kotlin
suspend fun dummy() {}

suspend fun main() {
    val lambda: suspend () -> Unit = {
       dummy()
       println(1)
       dummy()
       println(2)
    }
    lambda()
}
```
which, upon running, will print `1` and `2`, as expected.

One can call a suspend function only from other suspend function or suspend lambda, but it can call ordinary, non-suspendable functions. For example, both `dummy` and `println` are used only inside the lambda. Because one is not allowed to call suspendable functions from ordinary, we can imagine two worlds: suspendable and ordinary. Alternatively, one can consider them as being of two different colors, and we color the program by using the "suspend" modifier.

The lambda, creatively named `lambda`, contains two suspend calls (`dummy`) and one from the `main` function to the lambda itself,
but there is no suspension. Let us add it:
```kotlin
import kotlin.coroutines.*

var c: Continuation<Unit>? = null

suspend fun suspendMe() = suspendCoroutine<Unit> { continuation ->
    println("Suspended")
    c = continuation
}

suspend fun main() {
    val lambda: suspend () -> Unit = {
        suspendMe()
        println(1)
        suspendMe()
        println(2)
    }
    lambda()
}
```
Now, when we run the code, it prints `Suspended` and nothing else; it does not even finish the execution of the program, since `lambda` is, in fact, suspended, and it suspends `suspend fun main` as well.

To fix the issue with the suspension of `main`, we need to cross a boundary between suspendable and ordinary worlds and make
`main` ordinary, so, when it starts a coroutine, and the coroutine suspends, `main` does not. Since one cannot call a suspendable
function from an ordinary one, there are special functions, so-called coroutine builders, whose sole purpose is to create a coroutine, run it, and when it suspends, return execution to the caller.
Other than that, they act like other ordinary functions.
Let's name ours, I don't know, `builder`:

```kotlin
fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object: Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {
            result.getOrThrow()
        }
    })
}
```
A separate section explains the exact mechanism of starting a coroutine (in a broad sense) and how one can write their builders. For now, consider `builder` as a boilerplate to cross the worlds.

Now, when we change `main` to use the builder and not suspend itself
```kotlin
fun main() {
    val lambda: suspend () -> Unit = {
        suspendMe()
        println(1)
        suspendMe()
        println(2)
    }
    builder {
        lambda()
    }
}
```
and then run the example, it will print expected `Suspended`, but this time it will exit the program.

Additionally, when we change `main` to resume the lambda
```kotlin
import kotlin.coroutines.*

var c: Continuation<Unit>? = null

suspend fun suspendMe() = suspendCoroutine<Unit> { continuation ->
    println("Suspended")
    c = continuation
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object: Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {
            result.getOrThrow()
        }
    })
}

fun main() {
    val lambda: suspend () -> Unit = {
        suspendMe()
        println(1)
        suspendMe()
        println(2)
    }
    builder {
        lambda()
    }
    c?.resume(Unit)
}
```
it will print
```text
Suspended
1
Suspended
```
`lambda` is resumed and then suspended once more. If we add a couple more `c?.resume(Unit)`
```kotlin
fun main() {
    val lambda: suspend () -> Unit = {
        suspendMe()
        println(1)
        suspendMe()
        println(2)
    }
    builder {
        lambda()
    }
    c?.resume(Unit)
    c?.resume(Unit)
    c?.resume(Unit)
}
```
we will get
```text
Suspended
1
Suspended
2
Exception in thread "main" java.lang.IllegalStateException: Already resumed
```

The last line is what we get when we try to resume a finished continuation.

In this little example happens a lot. The rest of the section explains it bit by bit, starting with a state-machine.
