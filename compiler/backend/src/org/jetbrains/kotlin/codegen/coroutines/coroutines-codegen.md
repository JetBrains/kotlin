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

### State-Machine

The compiler turns sequential code into suspendable by using state machines. It distributes suspending calls between states in a 
state-machine. The relationship between the calls and the states is one-to-one: each call gets a state, and each state gets a call. The 
state ends with the call, and the compiler places all instructions preceding the call in the same state before the call. It places all 
instructions after the last call in a separate state.

For example, having
```kotlin
dummy()
println(1)
dummy()
println(2)
```
the compiler splits the code in the following way
```text
==========
dummy()
----------
println(1)
dummy()
----------
println(2)
==========
```
where function boundaries are represented by `==========` and state boundaries are represented by `----------`. The compiler after splitting 
the function generates the following code (simplified for now):
```kotlin
val $result: Any? = null
when(this.label) {
    0 -> {
        this.label = 1
        $result = dummy(this)
        if ($result == COROUTINE_SUSPENDED) return COROUTINE_SUSPENDED
        goto 1
    }
    1 -> {
        println(1)
        this.label = 2
        $result = dummy(this)
        if ($result == COROUTINE_SUSPENDED) return COROUTINE_SUSPENDED
        goto 2
    }
    2 -> {
        println(2)
        return Unit
    }
    else -> {
        throw IllegalStateException("call to 'resume' before 'invoke' with coroutine")
    }
}
```
Then it puts the state-machine inside the `invokeSuspend` function. Thus, in addition to the usual for lambda captured parameters, `<init>` 
and `invoke`, we have `label` field and `invokeSuspend` function.

At the beginning of the function `label`'s value is `0`. Before the call, we set it to `1`. During the call, two things can happen:
1. `dummy` returns a result, in this case, `Unit`. When this happens, execution continues as if it was sequential code, jumping to the next
 state.
2. `dummy` suspends. When a suspending function suspends, it returns the `COROUTINE_SUSPENDED` marker. So, if `dummy` suspends,
the caller also suspends by returning the same `COROUTINE_SUSPENDED`. Furthermore, all the suspend functions in call stack suspend, 
returning COROUTINE_SUSPEND, until we reach the coroutine builder function, which just returns.

Upon resume, `invokeSuspend` is called again, but this time `label` is `1`, so the execution jumps directly to the second state, and the 
caller does not execute the first call to `dummy` again. This way, the lambda's execution can be suspended and resumed. Thus the lambda
is turned into a coroutine, which is, by definition, is a suspendable unit of code.

That is the reason why we need to turn linear code into a state machine. 

On a closing note, the state machine should be flat; in other words, there should be no state-machine inside the state of a state-machine.
Otherwise, inner state-machine states will rewrite `label`, breaking the whole suspend-resume machinery and leading to weird behavior,
ranging from CCE to infinite loops. Similar buggy behavior happens when several suspending calls are in one state: when the first call
suspends, and then the execution resumes, skipping all the remaining code in the state. Both these bugs were quite frequent in the early
days of coroutines inlining.

#### JVM: Suspend Markers
To distinguish suspending calls from ordinary ones, the codegen (more specifically, `ExpressionCodegen`) puts so-called suspend markers 
around the calls. We need to generate markers both before the call and after it since callee can be `suspendCoroutineUninterceptedOrReturn` 
intrinsic, and we consider its call as a suspension point, i.e., a place where a coroutine can suspend. Thus, a suspension point is either 
a non-inline suspend call or an inlined `suspendCoroutineUninterceptedOrReturn` call.

It is important to note that there is no point in putting the markers around inline function calls since the compiler inlines their bodies, 
and their suspend points will become inline-side's suspension points. Remember, a state-machines should be flat, without nested 
state-machines.

The markers are calls of `kotlin.jvm.internal.InlineMarker.mark` with an integer parameter. The values for before and after suspending call 
markers are `0` and `1` respectively. Inline suspending functions keep the markers in generated bytecode so, upon inlining suspending calls, 
which they surround become suspension points. Well, at least in one copy of the inline function, to be technically correct. Because the 
compiler generates two copies of it: one with a state-machine, so one can call it via reflection; and the other one without, so the inliner 
can inline it without inlining the state-machine into another. Since there are libraries with inline suspend functions, the values passed 
to `kotlin.jvm.internal.InlineMarker.mark`'s call cannot be changed.

The codegen generates the markers by calling `addSuspendMarker`. After generating `MethodNode` of the function, it passes the node to 
`CoroutineTransformerMethodVisitor`. `CoroutineTransformerMethodVisitor` collects the suspension
points by checking these markers, and then it generates the state-machine.
FIXME: I should rename `CoroutineTransformerMethodVisitor` to `StateMachineBuilder` already.

#### JS & Native: Suspend Markers
The difference between JVM_IR and JS_IR/Native in regards to coroutine codegen non-JVM back-ends do not generate suspending markers in the 
resulting code. They still collect suspension points, however.
That is because they assume the closed-world model; in other words, they do not generate libraries in their target languages, which could 
contain suspending inline functions. Thus, the back-ends run the inliner before all lowerings, and they generate state machine during a 
suspend lowering, whereas JVM_IR still relies on the old back-end's `CoroutineTransfromerMethodVisitor` to do this since we cannot just 
generate state machine during a lowering, there are suspend inline functions in libraries bytecode, because of the open-world model.
Well, we can, if the function does not inline other functions. Nevertheless, there is much work to do in the new back-end, and generating a 
state-machine during the lowering is a part of it.

