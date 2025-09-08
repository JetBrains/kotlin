# Coroutines Codegen
This document aims to collect every piece of information about coroutines codegen in one place, so, instead of reading the compiler code or
writing snippets and looking at resulting bytecode, a programmer can check the document and find a section which explains how and, more
importantly, why the compiler behaves like this (or, to be precise, should behave like this). Hopefully, this will help people working on
the compiler and advanced Kotlin programmers to understand the reasons behind specific design decisions.

The document is JVM-centric, that means it explains how things work in JVM back-end since this is the area I am most familiar with and since in
JVM, there are guaranties of backward compatibility, which the compiler shall obey in both so-called "Old JVM" back-end, and in the new
JVM_IR one. The new back-end's naming can differ from the official documentation: the document uses the "IR" suffix, while the official
documentation omits it.

If the name of a section of the document has an "Old JVM:" prefix, it explains old JVM back-end specific details; if the prefix is "JVM_IR,"
then it is JVM_IR back-end specific. If the prefix is common "JVM," the explanation applies to both the old back-end and the new one. 
Otherwise, the section explains the general behavior of coroutines and shall apply to all back-ends.

The document sticks to release coroutines since we deprecated experimental coroutines in 1.3, and JVM_IR does not support them. Furthermore,
experimental coroutines support was dropped from compiler in 1.6.

If the current implementation is not ideal (or has a bug), there is a description of the difference, and the steps to implement the
"correct" version. These subsections start with "FIXME."

Throughout the document term "coroutine" will represent either a suspend lambda or a suspend function, which is different from the usual
definition of coroutines - something like a lightweight thread. The document reuses the term since "suspend lambda or function" is wordy,
and when it requires the typical definition, it says explicitly "a coroutine in a broad sense."

The document often uses the term "undefined behavior," which means that we consciously rejected defining it. Thus, the behavior may vary
from version to version, from back-end to back-end, and one should use it with extreme caution.

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

One can call a suspend function only from other suspend function or suspend lambda, but it can call ordinary, non-suspendable functions. For
example, both `dummy` and `println` are used only inside the lambda. Because one cannot call suspendable functions from ordinary, we can
imagine two worlds: suspendable and ordinary. Alternatively, one can consider them as two different colors, and we color the program by
using the "suspend" modifier.

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
Now, when we run the code, it prints `Suspended` and nothing else; it does not even finish the execution of the program, since `lambda` is,
in fact, suspended, and it suspends `suspend fun main` as well.

To fix the issue with the suspension of `main`, we need to cross a boundary between suspendable and ordinary worlds and make
`main` ordinary, so, when it starts a coroutine, and the coroutine suspends, `main` does not. Since one cannot call a suspendable
function from an ordinary one, there are special functions, so-called coroutine builders, whose sole purpose is to create a coroutine, run
it, and when it suspends, return execution to the caller.
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
A separate section explains the exact mechanism of starting a coroutine (in a broad sense) and writing their builders. For now, consider
`builder` as a boilerplate to cross the worlds.

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

A lot happens in this little example. The rest of the section explains it bit by bit, starting with a state-machine.

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
when (this.label) {
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
caller does not execute the first call to `dummy` again. This way, the lambda's execution can be suspended and resumed. Thus, the lambda
is turned into a coroutine, which is, by definition, is a suspendable unit of code.

That is the reason why we need to turn linear code into a state machine.

On a closing note, the state machine should be flat; in other words, there should be no state-machine inside a state-machine state.
Otherwise, inner state-machine states will rewrite `label`, breaking the whole suspend-resume machinery and leading to weird behavior,
ranging from ClassCastException to infinite loops. Similar buggy behavior happens when several suspending calls are in one state: when the first call
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
The difference between JVM_IR and JS_IR/Native regarding coroutine codegen non-JVM back-ends do not generate suspending markers in the
resulting code.
That is because they assume the closed-world model; in other words, they do not generate libraries in their target languages, which could
contain suspending inline functions. Thus, the back-ends run the inliner before all lowerings, and they generate state machine during a
suspend lowering, whereas JVM_IR still relies on the old back-end's `CoroutineTransfromerMethodVisitor` to do this since we cannot just
generate state machine during a lowering, there are suspend inline functions in libraries bytecode, because of the open-world model.
Well, we can, if the function does not inline other functions. Nevertheless, there is much work to do in the new back-end, and generating a
state-machine during the lowering is a part of it.

### Continuation Passing Style
The section about state-machines touched upon the `COROUTINE_SUSPENDED` marker and said that suspending functions and lambdas return the
marker when they suspend. Consequently, every suspend function return `returnType | COROUTINE_SUSPENDED`
union type. However, since neither Kotlin nor JVM support union types, every coroutine's return type is `Any?` (also known as
`java.lang.Object`) at runtime.

Let's now look to resume process closely. Suppose we have a couple of coroutines, one of them calls the other:
```kotlin
fun main() {
    val a: suspend () -> Unit = { suspendMe() }
    val b: suspend () -> Unit = { a() }
    builder { b() }
    c?.resume(Unit)
}
```
`suspendMe` here, as in the previous example, suspends. Stack trace inside `suspendMe` look like (skipping non-relevant parts)
```text
suspendMe
main$a$1.invokeSuspend
main$a$1.invoke
main$b$1.invokeSuspend
main$b$1.invoke
// ...
builder
main
```
as one can see, everything is as expected. `main` calls `builder`, which in turns calls `b.invoke`, and so on until `suspendMe`. Since
 `suspendMe` suspends, it returns `COROUTINE_SUSPENDED` to `a`'s `invokeSuspend`. As explained in the state-machine section checks, the
 caller checks that `suspendMe` returns `COROUTINE_SUSPENDED` and, in turn, returns `COROUTINE_SUSPENDED`. The same happens in all functions
 in call-stack in reverse order.

With the suspension process explained and out of the way, its counterpart - resumption - is next. When we call `c?.resume(Unit)`. `c` is,
technically, `a`, since `suspendMe` is a tail-call function (more on that in the relevant section). `resume` calls
`BaseContinuationImpl.resumeWith`. `BaseContinuationImpl` is a superclass of all coroutines, not user-accessible, but used for almost
everything coroutines-related that requires a class. It is the core of coroutine machinery, responsible for the resumption process.
`BaseContinuationImpl`, in turn, calls `a`'s `invokeSuspend`.

So, when we call `c?.resume(Unit)`, the stacktrace becomes
```text
main$a$1.invokeSuspend
BaseContinuationImpl.resumeWith
main
```

Now `a` continues its execution and returns `Unit`. But the execution returns to `BaseContinuationImpl.resumeWith`. However, we need to
continue the execution of `b` since `b` called `a`. In other words, we need to store a link to `b` somewhere in `a`, so then, inside
`BaseContinuationImpl.resumeWith`, we can call `b`'s `resumeWith`, which then resumes the execution of `b`. Remember, `b` is a coroutine,
and all coroutines inherit `BaseContinuationImpl`, which has the method `resumeWith`. Thus, we need to pass `b` to `a`. The only place where
we can pass `b` to `a` is the `invoke` function call. So, we add a parameter to `invoke`. `a.invoke`'s signature becomes
```kotlin
fun invoke(c: Continuation<Unit>): Any?
```

`Continuation` is a superinterface of all coroutines (unlike `BaseContinuationImpl`, it is user-accessible), in this case, suspend lambdas.
It is at the top of the inheritance chain. The type parameter of continuation is the old return type of suspending lambda.
The type parameter is the same as the type parameter of
`resumeWith`'s `Result` parameter: `resumeWith(result: Result<Unit>)`. One might recall from the `builder` example in the suspending lambda
section, where we create a continuation object. The object overrides `resumeWith` with the same signature.

Adding the `continuation` parameter to suspend lambdas and functions is known as Continuation-Passing Style, the style actively used in
lisps such as Scheme. For example, if a function returns a value in a continuation-passing style in Scheme, it passes the value to the continuation
parameter. So, a function accepts the continuation parameter, and the caller passes the continuation by calling `call/cc` intrinsic. The
same happens in Kotlin with passing return value to caller's continuation's `resumeWith`. However, unlike Scheme, Kotlin does not use
something like `call/cc`. Every coroutine already has a continuation. The caller passes it to the callee as an argument. Since the coroutine
passes the return value to `resumeWith`, its parameter has the same type as the coroutine's return type. Technically, the type is
`Result<T>`, but it is just a union `T | Throwable`; in this case, `T` is `Unit`. The next section uses return types other than `Unit` to
illustrate how to resume a coroutine with a value. The other part, `Throwable`, is for resuming a coroutine with an exception and is
explained in the relevant section.

After we passed parent coroutine's continuation to a child coroutine, we need to store it somewhere. Since "parent coroutine's
continuation" is quite long and mouthful for a name, we call it 'completion'. We chose this name because the coroutine calls it upon the
completion.

Since we add a continuation parameter to each suspend function and lambda, we cannot call suspending functions or lambdas
from ordinary functions, and we cannot call them by passing null as the parameter since the coroutine call `resumeWith` on it. Instead, we
should use coroutine builders, which provide root continuation and start the coroutine. That is the reason for the two worlds model.

#### Old JVM: getOrCreateJvmSuspendFunctionView
The old back-end uses `FunctionDescriptor` to represent suspending functions and lambdas with runtime signature. These are synthetic
descriptors, which are created by `getOrCreateJvmSuspendFunctionView`. The transformed one is named view. I could not find a reason for a
name, other than it is simple, original, and consistent. It generates this descriptor and stores
it in `bindingContext`, so it returns the same instance upon consequent calls. `unwrapInitialDescriptorForSuspendFunction` returns the
original descriptor.

The continuation parameter is named `continuation` in Kotlin Metadata and `$completion` in LVT.
FIXME: use consistent naming, rename `continuation` to `$completion`,

#### JVM_IR: AddContinuationLowering
In JVM_IR `AddContinuationLowering` is responsible for turning suspend functions into views. However, it keeps the return types of the
functions in the views' `IrFunction`, so it only adds the continuation parameter. Using the original return type simplifies tail-call
optimization. Specifically, it simplifies tail-call optimization analysis for functions returning `Unit`. The codegen, however, generates
them as returning `Any?`.

The continuation parameter is named `$completion` in both Kotlin Metadata and LVT.

### Resume With Result
Let us consider the following example with a suspending function, returning a value, instead of `Unit`:
```kotlin
import kotlin.coroutines.*

var c: Continuation<Int>? = null

suspend fun suspendMe(): Int = suspendCoroutine { continuation ->
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
    val a: suspend () -> Unit = { println(suspendMe()) }
    builder { a() }
    c?.resume(42)
}
```
if one runs the program, it prints `42`. However, `suspendMe` does not return `42`. It just suspends and returns nothing. By the way,
`suspendMe`'s continuation has type `Continuation<Int>`, i.e., the compiler moves function's return to type argument of the `Continuation`
interface I mentioned in the previous section (about continuation-passing style).

The state-machine section touched upon the `$result` variable inside the `invokeSuspend` function. The listing shows the `invokeSuspend`
function of `a`, but, unlike the previous example, with its signature:
```kotlin
fun invokeSuspend($result: Any?): Any? {
    when (this.label) {
        0 -> {
            this.label = 1
            $result = suspendMe(this)
            if ($result == COROUTINE_SUSPENDED) return COROUTINE_SUSPENDED
            goto 1
        }
        1 -> {
            println($result)
            return Unit
        }
        else -> {
            throw IllegalStateException("call to 'resume' before 'invoke' with coroutine")
        }
    }
}
```
The listing shows that the `$result` variable is both parameter of the function and result of suspending call. Thus, when we call
`c?.resume(42)`, the value `42` is passed to `BaseContinuationImpl.resumeImpl`, it calls `invokeSuspend` with it. Now, since `label`'s value
is `1` (`suspendMe` suspended),
`42` is printed. Note that in the first state, we ignore the argument of `invokeSuspend`, and this becomes important when we
see how we start a coroutine.

So, what happens, when we call `resume` inside `suspendCoroutine`? Like
```kotlin
suspendCoroutine<Int> { it.resume(42) }
```
Following the resume process, `resume` calls continuation's `resumeWith`, which calls `invokeSuspend` with value `42`. `$result` then
contains the value and work the same as if `suspendMe` returned `42`. In other words, `suspendCoroutine` with an unconditional resume will
not suspend the coroutine and is semantically the same as returning the value.

It is important to note that passing `COROUTINE_SUSPENDED` to continuation's `resumeWith` leads to undefined behavior.

### Resume with Exception
After reading the previous section about resume with a value, one might assume that `$result`'s type is `Int | COROUTINE_SUSPENDED`, but
this is not completely true. It is `Int | COROUTINE_SUSPENDED | Result$Failue(Throwable)`, or, more generally, it is `returnType |
COROUTINE_SUSPENDED | Result$Failue(Throwable)`. The section covers the last part: `Result$Failue(Throwable)`.

Let us change the previous example to resume the coroutine with exception:
```kotlin
import kotlin.coroutines.*

var c: Continuation<Int>? = null

suspend fun suspendMe(): Int = suspendCoroutine { continuation ->
    c = continuation
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object: Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {
            println(result.exceptionOrNull())
        }
    })
}

fun main() {
    val a: suspend () -> Unit = { println(1 + suspendMe()) }
    builder { a() }
    c?.resumeWithException(IllegalStateException("BOO"))
}
```
which, upon running, will print the exception. Note, that it is printed inside the `builder` function
(because of `println(result.exceptionOrNull())`). There are a couple of things happening here: inside the generated state machine and inside
`BaseContinuationImpl`'s `resumeWith`.

First, we change the generated state machine. As explained before, the type of `$result` variable is `Int | COROUTINE_SUSPENDED |
Result$Failue(Throwable)`, but when we resume, by convention, its type cannot be `COROUTINE_SUSPENDED`. Still, the type is `Int |
Result$Failure(Throwable)`, which we cannot just pass to `plus`, at least, without a check and `CHECKCAST`. Otherwise, we will get CCE at
runtime. Thus, we check the `$result` variable and throw the exception if the variable holds it.
```kotlin
fun invokeSuspend($result: Any?): Any? {
    when (this.label) {
        0 -> {
            this.label = 1
            $result = suspendMe(this)
            if ($result == COROUTINE_SUSPENDED) return COROUTINE_SUSPENDED
            goto 1
        }
        1 -> {
            $result.throwOnFailure()
            println(1 + $result)
            return Unit
        }
        else -> {
            throw IllegalStateException("call to 'resume' before 'invoke' with coroutine")
        }
    }
}
```
where `throwOnFailure` is a function that performs the check and throwing part for us.

Now, when we throw the exception, it should end up in the `main` function. However, as we saw from the example, it comes to `builder`'s
root continuation's `resumeWith`. The builder creates the root continuation, and, unlike other continuations, it has no completion. We
expect it to reach root continuation, since when we call one suspending function or lambda from another, we want to propagate the exception
through suspending stack (also known as an async stack), from callee to caller, regardless of whether or not there was a suspension, unless,
there is an explicit try-catch block. Thankfully, we can propagate the exception the same way as the execution upon coroutine's completion,
through the chain of `completion` fields. We, after all, should pass it to the caller, just like the return value. When `invokeSuspend`
throws an exception, `BaseContinuationImpl.resumeWith` catches it, wraps into `Result` inline class, which is essentially `T |
Result$Failure(Throwable)`, and calls `completion`'s `resumeWith` with the result (simplified):
```kotlin
abstract class BaseContinuationImpl(
    private val completion: Continuation<Any?>
): Continuation<Any?> {
    public final override fun resumeWith(result: Result<Any?>) {
        val outcome = try {
            val outcome = invokeSuspend(result)
            if (outcome == COROUTINE_SUSPENDED) return
            Result.success(outcome)
        } catch (e: Throwable) {
            Result.failure(e)
        }
        completion.resumeWith(outcome)
    }

    protected abstract fun invokeSuspend(result: Result<Any?>): Any?
}
```
The function passes the exception to `invokeSuspend`, `invokeSuspend` calls `throwOnFailure` and throws it again, then the exception is
caught in `BaseContinuationImpl.resumeWith` and wrapped again until it reaches root continuation's `resumeWith`, where, in this case, the
coroutine builder prints it. By the way, `resumeWithException` works in release coroutines precisely in the same way (except the catching
part): it wraps the exception into `Result` like in a burrito. It passes it to continuation's `resumeWith`. `resume` also wraps the argument
into `Result` and passes it to `resumeWith`.

### Variables Spilling
All the previous examples did not have local variables, and there is a reason for it. When a coroutine suspends, we should save its local
variables. Otherwise, when it resumes, the values of them are lost. So, before the suspension, which can be on each suspend call (more
generally, on each suspension point), we save them, and after the resumption, we restore them. There is no reason to restore them right
after the call if the call did not return `COROUTINE_SUSPENDED`: their values are still in local variable slots.

Let us consider a simple example:
```kotlin
import kotlin.coroutines.*

data class A(val i: Int)

var c: Continuation<Unit>? = null

suspend fun suspendMe(): Unit = suspendCoroutine { continuation ->
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

suspend operator fun A.plus(a: A) = A(i + a.i)

fun main() {
    val lambda: suspend () -> Unit = {
        val a1 = A(1)
        suspendMe()
        val a2 = A(2)
        println(a1 + a2)
    }
    builder {
        lambda()
    }
    c?.resume(Unit)
}
```
We should save `a1` before `suspendMe`, and we should restore it after the resumption. Similarly, we should save both `a1` and `a2`
before `+`, since the compiler does not generally know whether suspend call will suspend, so it assumes that the suspension might happen in
each suspension point. So, it spills the locals before each call and unspills after it.

Thus, the compiler generates the following state machine
```kotlin
fun invokeSuspend($result: Any?): Any? {
    when (this.label) {
        0 -> {
            var a1 = A(1)
            this.L$0 = a1
            this.label = 1
            $result = suspendMe()
            if ($result == COROUTINE_SUSPENDED) return COROUTINE_SUSPENDED
            goto 1_1
        }
        1 -> {
            a1 = this.L$0

            1_1:
            var a2 = A(2)
            this.L$0 = null
            this.label = 2
            $result = plus(a1, a2)
            if ($result == COROUTINE_SUSPENDED) return COROUTINE_SUSPENDED
            goto 2_1
        }
        2 -> {
            2_1:
            println($result)
            return Unit
        }
        else -> {
            throw IllegalStateException("call to 'resume' before 'invoke' with coroutine")
        }
    }
}
```

As one can see, the generated code does not spill and unspill variables, which are dead, in other words, which are not required afterward.
Furthermore, it cleans the field for spilled variables of reference types to avoid memory leaks by pushing `null` to it so that GC can
collect the object.

#### Spilled Variables Naming
One might notice that the names of the fields for spilled variables are odd. The naming scheme is the following: the first letter of the
name represents type descriptor of variable:
* L for a reference type, i.e., objects and arrays
* J for longs
* D for doubles
* F for floats
* I for booleans, bytes, chars, shorts, and ints

It is important to note that although in Java Bytecode, we represent boolean variables with integer type and on HotSpot assigning a boolean
variable to the field of type `int` is fine, on Dalvik, these types are distinct. Thus, we coerce non-integer primitive integral types
(except long) before using them.

The second letter is `$`, which is unlikely to be used in user code. We cannot start spilled variables with `$`, since the compiler uses
the prefix `$` for captured variables and using the same prefix for multiple things would confuse the inliner.

The rest is just the integer index of the variable with the same prefix. I.e., there can be variables `I$0`, `L$0` and `L$1` inside the
same suspend lambda object.

#### Spilled Variables Cleanup
Since we spill a reference to the continuation object, we now hold an additional reference to the object. Thus, GC cannot clean its memory
as long as there is a reference to the continuation. Of course, holding a reference to a not-needed object leads to memory leaks. The
compiler clears the fields for reference types up to avoid the leaks.

Consider the following example:
```kotlin
suspend fun blackhole(a: Any?) {}

suspend fun cleanUpExample(a: String, b: String) {
    blackhole(a) // 1
    blackhole(b) // 2
}
```
After line (1) `a` is dead, but `b` is still alive. So, we spill only `b`, and instead of `a` we spill `null` by default. 
There is no variable alive after line (2), but the continuation
object still holds a reference to `b` in the `L$0` field. So, to clean it up and avoid memory leaks, we push `null` to it.

If API Version >= 2.2, 
the `null` spilling process is done via calls to `kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable` function and
pushing the return value to the continuation field. By default, it just returns null, but debugger is expected to replace the body
of the function with one returning its arguments, so the variable will be visible even when it is dead. This way, we both avoid 
memory leaks and make the code debuggable.

If API Version < 2.2, the compiler just pushes `null` to continuations, unless `-Xdebug` flag is used. When the flag is used,
the compiler does nothing in terms of variable cleanup. Without the flag, the compiler does not spill dead variables, including
arguments, removes LVT records for them, and shrinks LVT records for alive variables, so the debugger will not see dead variables.

Generally, the compiler generates spilling and unspilling code so that it uses only the first fields. If there are M fields for references,
but we spill only N (where N ≤ M, of course) objects at the suspension point, everything else should be `null`. However, we do not need to
nullify all of them every suspension point. Instead, the compiler checks which of the fields hold references and clears only them.

#### Stack spilling
In the previous examples, the stack was clean before a call, meaning that there were only call arguments before the call, and only the call
result is on the stack after the call.

However, this is not always true. Consider the following example:
```kotlin
val lambda: suspend () -> Unit = {
    val a1 = A(1)
    val a2 = A(2)
    val a3 = A(3)
    a1 + (a2 + a3)
}
```
and have a closer look at `a1 + (a2 + a3)` expression. If `+` were not suspend, the compiler would generate the following
bytecode:
```text
ALOAD 1 // a1
ALOAD 2 // a2
ALOAD 3 // a3
INVOKESTATIC plus
INVOKESTATIC plus
ARETURN
```
We cannot just make this code suspendable since, after the resumption, the stack has only `$result` (it is passed to `resumewith` and is the
argument of `invokeSuspend`). So, there are not enough variables on the stack for the second call. Consequently, we need to save the stack
before the call and then restore it after the call. Instead of creating the separate logic of stack into slots spilling, we reuse two
already existing ones. One is stack normalization, which is already present in inliner. The inliner spills the stack into locals before the
inline call and restores them after the call.
So, if we do the same here, the bytecode becomes

```text
INVOKESTATIC InlineMarker.beforeInlineCall
ALOAD 1 // a1
INVOKESTATIC InlineMarker.beforeInlineCall
ALOAD 2 // a2
ALOAD 3 // a3
ICONST 0
INVOKESTATIC InlineMarker.mark
INVOKESTATIC plus
ICONST 1
INVOKESTATIC InlineMarker.mark
INVOKESTATIC InlineMarker.afterInlineCall
ICONST 0
INVOKESTATIC InlineMarker.mark
INVOKESTATIC plus
ICONST 1
INVOKESTATIC InlineMarker.mark
INVOKESTATIC InlineMarker.afterInlineCall
ARETURN
```
where suspend markers are `ICONST (0|1) INVOKESTATIC InlineMarker.mark`; and after stack normalization (`FixStackMethodTransformer`
normalizes the stack), the bytecode looks like
```text
ALOAD 1 // a1
ASTORE 4 // a1
ALOAD 2 // a2
ALOAD 3 // a3
ICONST 0
INVOKESTATIC InlineMarker.mark
INVOKESTATIC plus
ICONST 1
INVOKESTATIC InlineMarker.mark
ASTORE 5 // a2 + a3
ALOAD 4 // a1
ALOAD 5 // a2 + a3
ICONST 0
INVOKESTATIC InlineMarker.mark
INVOKESTATIC plus
ICONST 1
INVOKESTATIC InlineMarker.mark
ARETURN
```
we need to spill `a2 + a3` since we should preserve the order of `plus`'s arguments. So, along with the suspend markers, the codegen puts
inline markers. However, unlike suspend markers, they are put around call arguments as well. So, the order the codegen generates
suspendable calls in the following:
1. `beforeInlineCall` marker
2. arguments
3. before suspendable call marker
4. the call itself
5. after suspendable call marker
6. `afterInlineCall` marker

If we look at stack normalization once more, we see that there are now five locals, but, thankfully, we do not spill all of them. `a2 + a3`
is not alive during both calls and is not present in LVT, so there is no reason for the compiler to spill it. The same applies for slot 4:
the variable
is dead during the second call, so we spill it only once. `a2` and `a3`  are dead during both calls, and thus they are not spilled, as well
as `a1` during the second call.

FIXME: do not spill the same variables multiple times. We can reuse one spilled variable and put it to several slots. Even better, do not
create new locals while spilling the stack. In this example, `ALOAD 1` can be removed, thus removing the need in `ALOAD 4` So, the ideal
bytecode will look like
```text
ALOAD 2 // a2
ALOAD 3 // a3
ICONST 0
INVOKESTATIC InlineMarker.mark
INVOKESTATIC plus
ICONST 1
INVOKESTATIC InlineMarker.mark
ASTORE 4 // a2 + a3
ALOAD 1 // a1
ALOAD 4 // a2 + a3
ICONST 0
INVOKESTATIC InlineMarker.mark
INVOKESTATIC plus
ICONST 1
INVOKESTATIC InlineMarker.mark
ARETURN
```
Then we will have the same three locals to spills, instead of four.

### Coroutine Intrinsics
Previous examples had an elementary coroutine builder. They used so-called empty continuation. Let us now recreate kotlinx.coroutines'
`async` function, which runs a coroutine on another thread and then, upon its completion, returns the result to the main thread.

First, we need a class which waits on the main thread:
```kotlin
class Async<T> {
    suspend fun await(): T = TODO()
}
```
then the root continuation:
```kotlin
class AsyncContinuation<T>: Continuation<T> {
    override val context = EmptyCoroutineContext

    override fun resumeWith(result: Result<T>) {
        TODO()
    }
}
```
Now, we can piece these two classes together. In `await`, we should check whether a coroutine already computed the result and if so, return
it by using `it.resume(value)` trick from resume with result section. Otherwise, we should save the continuation, so it can be resumed when
the result is available. Inside the continuation's `resumeWith`, we should check whether we await the result and resume the awaiting
continuation with the computed result; otherwise, we save the result so that it will be accessible in `await`. In code, it will look like:
```kotlin
class AsyncContinuation<T>: Continuation<T> {
    var result: T? = null
    var awaiting: Continuation<T>? = null
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resumeWith(result: Result<T>) {
        if (awaiting != null) {
            awaiting?.resumeWith(result)
        } else {
            this.result = result.getOrThrow()
        }
    }
}

class Async<T>(val ac: AsyncContinuation<T>) {
    suspend fun await(): T =
        suspendCoroutine<T> {
            val res = ac.result
            if (res != null)
                it.resume(res)
            else
                ac.awaiting = it
        }
}
```

Finally, we can write the builder itself:
```kotlin
fun <T> async(c: suspend () -> T): Async<T> {
    val ac = AsyncContinuation<T>()
    c.startCoroutine(ac)
    return Async(ac)
}
```
and simple main function to test, that everything works as expected (again, check with suspension just to be sure we did not miss
anything):
```kotlin
fun main() {
    var c: Continuation<String>? = null
    builder {
        val async = async {
            println("Async in thread ${Thread.currentThread().id}")
            suspendCoroutine<String> { c = it }
        }
        println("Await in thread ${Thread.currentThread().id}")
        println(async.await())
    }
    c?.resume("OK")
}
```
Upon running, it will print
```text
Async in thread 1
Await in thread 1
OK
```
Since it is not multithreaded yet, it will run `async`'s coroutine in the main thread. However, before we make it multithreaded, we need to
cover how the `suspendCoroutine` function works.

#### suspendCoroutine
After explaining in depth how resume works, which hoops the compilers jumps through to generate a (correct) state machine, let's see, what
happens, when we call `suspendCoroutine`. We now know two pieces about the function: it somehow returns `COROUTINE_SUSPENDED` and it
provides access to continuation parameter. The function is defined as follows:
```kotlin
public suspend inline fun <T> suspendCoroutine(crossinline block: (Continuation<T>) -> Unit): T =
    suspendCoroutineUninterceptedOrReturn { c: Continuation<T> ->
        val safe = SafeContinuation(c.intercepted())
        block(safe)
        safe.getOrThrow()
    }
```
So, it does five different things:
1. accesses continuation argument
2. intercepts continuation
3. wraps it in `SafeContinuation`
4. calls the lambda argument
5. returns result, `COROUTINE_SUSPENDED` or throws an exception

#### suspendCoroutineUninterceptedOrReturn
First, let us examine how one can access a continuation argument without suspending current executions.
`suspendCoroutineUninterceptedOrReturn` is an intrinsic function that does only one thing: inlines provided lambda parameter passing
continuation parameter to it. Its purpose is to give access to the continuation argument, which is invisible in suspend functions
and lambdas. Thus we cannot write in pure Kotlin. It has to be intrinsic.

Fun fact: since the lambda returns `returnType | COROUTINE_SUSPENDED`, the compiler does not check its return type, so there can be some
funny CCEs at runtime because of this unsoundness in the Koltin type system:
```kotlin
import kotlin.coroutines.intrinsics.*

suspend fun returnsInt(): Int = suspendCoroutineUninterceptedOrReturn { "Nope, it returns String" }

suspend fun main() {
    1 + returnsInt()
}
```
will throw
```text
Exception in thread "main" java.lang.ClassCastException: java.lang.String cannot be cast to java.lang.Number
```

Furthermore, the runtime throws the CCE upon the usage of the return value. So, if one just ignores its return value or even better (I mean,
worse) calls the function in tail-call position (to enable tail-call optimization), no exception is thrown. So, the next example
runs just fine:
```kotlin
import kotlin.coroutines.intrinsics.*

suspend fun returnsInt(): Int = suspendCoroutineUninterceptedOrReturn { "Nope, it returns String" }

suspend fun alsoReturnsInt(): Int = returnsInt()

suspend fun main() {
    returnsInt()
    alsoReturnsInt()
}
```

#### SafeContinuation
Of course, there is a reason for `SafeContinuation`. Let's consider the following example:
```kotlin
fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object: Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {
            result.getOrThrow()
        }
    })
}

fun main() {
    builder {
        suspendCoroutineUninterceptedOrReturn {
            it.resumeWithException(IllegalStateException("Boo"))
        }
    }
}
```
One might assume, that we will get `IllegalStateException`, but this in not what happens here:
```text
Exception in thread "main" kotlin.KotlinNullPointerException
   at kotlin.coroutines.jvm.internal.ContinuationImpl.releaseIntercepted(ContinuationImpl.kt:118)
   at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:39)
   at kotlin.coroutines.ContinuationKt.startCoroutine(Continuation.kt:114)
```
That is an example of undefined behavior.

So, what happens here and why it causes the KNPE? When we call `resumeWithException`, inside `BaseContinuationImpl.resumeWith` we call
`releaseIntercepted`, where we set `intercepted` field to `CompletedContinuation`:
```kotlin
protected override fun releaseIntercepted() {
    val intercepted = intercepted
    if (intercepted != null && intercepted !== this) {
        context[ContinuationInterceptor]!!.releaseInterceptedContinuation(intercepted)
    }
    this.intercepted = CompletedContinuation // just in case
}
```
Then, when we throw the exception by calling `getOrThrow`, `BaseContinuationImpl.resumeWith` catches it (see the section about resume with
exception), and calls `releaseIntercepted` again, but since there is no continuation interceptor in `context`, we get the KNPE.

That is what essentially `SafeContinuation` prevents. It catches an exception inside its `resumeWith` method and saves it until
`suspendCoroutine` calls `getOrThrow`. Also, `getOrThrow` returns `COROUTINE_SUSPENDED` for not-yet-finished coroutines. In other words,
when a wrapped coroutine suspends, `getOrThrow` tells `suspendCoroutine` to suspend.

### startCoroutine
We have already covered how a coroutine suspends, what happens when it resumes and how the compiler handles it. However, we have never
looked at how one can create or start a coroutine. In all previous examples, one could notice a call to `startCoroutine`. There are two
versions of the function: to start a suspend lambda without parameters and to start a coroutine with either one parameter or a receiver. It
is defined as follows:
```kotlin
public fun <T> (suspend () -> T).startCoroutine(completion: Continuation<T>) {
    createCoroutineUnintercepted(completion).intercepted().resume(Unit)
}
```
So, it
1. creates a coroutine
2. intercepts it
3. starts it

Once again, `createCoroutineUnintercepted` has two versions - without parameters and with exactly one parameter. All it does is calling
suspending lambda's `create` function. After the interception, we resume the coroutine with a dummy value. As explained in the resume with
the value section, the state-machine ignores its first state value. Thus, it is the perfect way to start a coroutine without calling
`invokeSuspend`. However, the way we start callable references is different. Since they are tail-call, in other words, do not have
a continuation inside an object, we wrap them in a hand-written one.

#### create
`create` is generated by the compiler, and it
1. creates a copy of the lambda by calling a constructor with captured variables
2. puts `create`'s arguments into parameter fields.

For example, if we have a lambda like
```kotlin
fun main() {
    val i = 1
    val lambda: suspend (Int) -> Int = { i + it }
}
```
the generated `create` will look like
```kotlin
public fun create(value: Any?, completion: Continuation): Continuation {
    val result = main$lambda$1(this.$i, completion)
    result.I$0 = value as Int
}
```
note that the constructor, in addition to captured parameters, accepts a completion object.

In Old JVM BE, `create` is generated for every suspend lambda even when we do not need it. I.e., even for suspending lambdas with
more than one parameter. There are only two versions of `createCoroutineUnintercepted`, and there are no other places where we call
`create` (apart from compiler-generated `invoke`s). Thus, in JVM_IR BE, we fixed the slip-up, and it generates the `create` function only
for functions with zero or one parameter.

##### Lambda Parameters
We need to put the suspend lambda arguments into fields since there can be only one argument of `invokeSuspend` - `$result`. The compiler
moves the lambda body into `invokeSuspend`. Thus, `invokeSuspend` does all the computation. We reuse fields for spilled variables for
parameters as well. For example, if we have a lambda with type `suspend Int.(Long, Any) -> Unit`, then `I$0` holds value of extension
receiver, `J$0` - the first argument, `L$1` - the second one.

This way, we can reuse spilled variables cleanup logic for parameters. If we used separate fields for parameters, we would need to manually
push `null` to them as we do for spilled variable fields if we do not need them anymore.

We unspill the lambda parameter before state-machine in `invokeSuspend` - this way, their values are visible in debugger.

If lambda parameter is unboxed inline class with reference underlying type, we store only unboxed value.

We store the parameters in `create`, if there is one, or in `invoke`'s mangled function.

#### invoke
`invoke` is basically `startCoroutine` without an interception. In `invoke`, we call `create` and resume a new instance with dummy value by
calling `invokeSuspend`. We cannot just call `invokeSuspend` without calling the constructor first because that would not create a
continuation needed for the completion chain, as explained in the continuation-passing style section. Also, recursive suspend lambda calls
would reset `label`'s value.

FIXME: We do not need to create an additional copy of the lambda if we can verify that we do not pass them as completion to themselves.
However, this includes not only recursive lambdas. We can pass the lambda to a tail-call suspending function and call it there. In this
case, the continuation object is the same, and we have the same problems as if there was a recursion.

Of course, in JVM_IR, we do not have a `create` function in case when the lambda has more than one parameter, `invoke` creates a new
instance of the lambda with copies of all captured variables and then puts the parameters of the lambda to fields.

#### Interception
After all this boring theory, we can finally turn our `async` example from the previous section into a multithreaded one. In all previous
examples I used `EmptyCoroutineContext` as `context` for root continuations. `CoroutineContext`, the type of `context` property, is
essentially a hash map from `CoroutineContext.Key` to `CoroutineContext.Element`. A programmer can store coroutine-local information in it,
and here 'coroutine' is used in a broad sense to represent a lightweight thread, not just a suspend function or a suspend lambda. So, one
can view `context` as a replacement of `ThreadLocal`. To access it, the user should use `coroutineContext` intrinsic. Even a single context
element is a context itself, so it forms a tree. The fact that one element of the context is context itself comes in handy when we need to
move a coroutine from one thread to another, i.e., intercept it. In order to do that, we need to provide the key and the element to the
context. There is a special interface `ContinuationInterceptor`, which overrides `CoroutineContext.Element` and has a property `key`.
Let us create one:
```kotlin
object SingleThreadedInterceptor: ContinuationInterceptor {
    override val key = ContinuationInterceptor.Key

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
        SingleThreadedContinuation(continuation)
}
```

In its method `interceptContinuation` we simply wrap provided continuation with a new one, and in this continuation we can run the coroutine
on a different thread:
```kotlin
class SingleThreadedContinuation<T>(val c: Continuation<T>): Continuation<T> {
    override val context: CoroutineContext
        get() = c.context

    override fun resumeWith(result: Result<T>) {
        thread {
            c.resumeWith(result)
        }
    }
}
```
Inside the `resumeWith` function, as one can see, we simply resume the continuation on another thread.

Note that we pass the `context` of provided continuation as our own, so our continuation inherits it from the wrapped one. That is not
required, but since `context` is a replacement for `ThreadLocal`, we should keep it. All we are allowed to do is add additional
infrastructural information, like `ContinuationInterceptor`, but we can never remove anything added by the user.

It is important to note that the `key` property should be constant. Otherwise, `get` on this key will return null, and there will be no
interception.

Now, if we change `AsyncContinuation`, `async` function and `main` to use the interceptor:
```kotlin
class AsyncContinuation<T>(override val context:  CoroutineContext): Continuation<T> {
    var result: T? = null
    var awaiting: Continuation<T>? = null

    override fun resumeWith(result: Result<T>) {
        if (awaiting != null) {
            awaiting?.resumeWith(result)
        } else {
            this.result = result.getOrThrow()
        }
    }
}

fun <T> async(context: CoroutineContext = EmptyCoroutineContext, c: suspend () -> T): Async<T> {
    val ac = AsyncContinuation<T>(context)
    c.startCoroutine(ac)
    return Async(ac)
}

fun main() {
    var c: Continuation<String>? = null
    builder {
        val async = async(SingleThreadedIntercepted) {
            println("Async in thread ${Thread.currentThread().id}")
            suspendCoroutine<String> { c = it }
        }
        println("Await in thread ${Thread.currentThread().id}")
        println(async.await())
    }
    c?.resume("OK")
}
```
and when we run the program, we get something like
```text
Async in thread 11
Await in thread 1
OK
```
as expected.

But what part of coroutine machinery calls `interceptContinuation` function of the interceptor? The function wraps the continuation, but
who calls the function? Well, `intercepted` does. If we rewrite
`async` as
```kotlin
fun <T> async(context: CoroutineContext = EmptyCoroutineContext, c: suspend () -> T): Async<T> {
    val ac = AsyncContinuation<T>(context)
    c.createCoroutineUnintercepted(ac)
//        .intercepted()
        .resume(Unit)
    return Async(ac)
}
```
(note, that I commented `intercepted` call out) and then run the example, we get
```text
Async in thread 1
Await in thread 1
OK
```
since without interception, we do not wrap the continuation to run the coroutine on another thread.

But how `intercepted` does that? Well, `intercepted`, after some indirections, does the following:
```kotlin
context[ContinuationInterceptor]?.interceptContinuation(this)
```
remember, `CoroutineContext.Element` is itself a `CoroutineContext` with a single element, which returns itself on `get` if its key is the
same as the provided one. That is why it is important to use constants as keys. We also cache intercepted continuation in the `intercepted`
field. The field causes KNPE when we do not wrap the continuation with `SafeContinuation`.

#### Restricted Suspension
There are cases when we do not want to allow calling other suspend functions or lambdas from ours, for example, inside a lambda, passed
to `sequence` function, we want to call only `yield` and `yieldAll` functions, unless the functions we call inside the lambda, call `yield`
or `yieldAll`. Furthermore, we do not want to intercept their continuations. We want to limit them to the main thread. In this case, we use
`@RestrictsSuspension` annotation on classes or interfaces, which contain leaf suspend functions, which the lambda allowed to call.
If we look at `sequence`, the `SequenceScope` interface is annotated with the annotation.

Since we do not want to intercept the continuations, their `context`s cannot be other than `EmptyCoroutineContext`.

### Coroutine Superclasses
Below is the diagram of all continuation classes, defined in the standard library and used by the compiler:
```text
   +------------+
   |Continuation|
   +------+-----+
          ^
          |
+---------+----------+
|BaseContinuationImpl+<---------------+
+---------+----------+                |
          ^                           |
          |                           |
  +-------+--------+    +-------------+------------+
  |ContinuationImpl|    |RestrictedContinuationImpl|
  +-------+--------+    +-------------+------------+
          ^                           ^
          |                           |
    +-----+-------+       +-----------+-----------+
    |SuspendLambda|       |RestrictedSuspendLambda|
    +-------------+       +-----------------------+
```

The main superinterface of all coroutines is `Continuation`:
```kotlin
public interface Continuation<in T> {
    public val context: CoroutineContext
    public fun resumeWith(result: Result<T>)
}
```
which is the only interface accessible by users. It is, essentially, the core of the coroutines' machinery. With the continuation-passing
style, every suspending function and lambda accept additional continuation parameter.

Every compiler generated continuation extends `BaseContinuationImpl`:
```kotlin
abstract class BaseContinuationImpl(
    public val completion: Continuation<Any?>?
) : Continuation<Any?>, CoroutineStackFrame, Serializable {
    public final override fun resumeWith(result: Result<Any?>)
    protected abstract fun invokeSuspend(result: Result<Any?>): Any?
    protected open fun releaseIntercepted()
    public open fun create(completion: Continuation<*>): Continuation<Unit>
    public open fun create(value: Any?, completion: Continuation<*>): Continuation<Unit>
    public override fun toString(): String
    public override val callerFrame: CoroutineStackFrame?
    public override fun getStackTraceElement(): StackTraceElement?
}
```
Note, its `resumeWith` function is final, but it introduces the `invokeSuspend` function. The `resumeWith` function does
the following:
1. When the user calls its `resumeWith`, it calls `invokeSuspend` with the argument, resuming the suspended coroutine with passed result or
exception.
2. It calls `completion` continuation's `resumeWith` when the coroutine completes so that execution returns to the caller.
3. It catches exceptions and resumes the `completion` with it, wrapped in `Result`, thus propagating the exception to the caller.

Additionally, `resumeWith` calls `releaseIntercepted` upon coroutine's completion to clear the interceptor up.

The compiler generates `create` overrides for suspending lambdas with zero or one parameter, since `createCoroutineUnintercepted` calls
them.

The rest (`callerFrame` and `getStackTraceElement`) come from the `CoroutineStackFrame` interface, and the debugger and kotlinx.coroutines
library use the interface to generate async stack traces.

The next class is `ContinuationImpl`. Every continuation of a suspending function, generated by the compiler, extends this class. Note that
the compiler does not generate restricted suspend functions (yet).
```kotlin
abstract class ContinuationImpl(
    completion: Continuation<Any?>?,
    private val _context: CoroutineContext?
) : BaseContinuationImpl(completion) {
    public override val context: CoroutineContext
    protected override fun releaseIntercepted()

    private var intercepted: Continuation<Any?>?
    public fun intercepted(): Continuation<Any?>
}
```
It adds the `intercepted` field and `intercepted()` function covered in the corresponding section.

For restricted suspend function, there is`RestrictedContinuationImpl` class, and thus their context can only be `EmptyCoroutineContext`. It
allows us to save several bytes when one calls `startCoroutine` on a suspending functional type, which does not inherit
`BaseContinuationImpl`. For example, when
the receiver is a callable reference to suspend functions
the context of root continuation, passed to `startCoroutine` is `EmptyCoroutineContext`
```kotlin
abstract class RestrictedContinuationImpl(
    completion: Continuation<Any?>?
) : BaseContinuationImpl(completion) {
    public override val context: CoroutineContext
}
```

All non-restricted generated suspend lambdas extend `SuspendLambda`:
```kotlin
internal abstract class SuspendLambda(
    public override val arity: Int,
    completion: Continuation<Any?>?
) : ContinuationImpl(completion), FunctionBase<Any?>, SuspendFunction
```
since all suspend lambdas are functional types, they implement the `FunctionBase` interface. `SuspendFunction` is a marker interface used in
type checks and type conversions (see the next subsection).

All restricted generated suspend lambdas extend `RestrictedSuspendLambda`:
```kotlin
abstract class RestrictedSuspendLambda(
    public override val arity: Int,
    completion: Continuation<Any?>?
) : RestrictedContinuationImpl(completion), FunctionBase<Any?>, SuspendFunction
```
the only difference from `SuspendLambda` is superclass. `SuspendLambda` inherits `ContinuationImpl`, while
`RestrictedSuspendLambda` inherits `RestrictedContinuationImpl`.

#### SuspendFunction{N}
Every suspending lambda has a special suspend functional type: `SuspendFunction{N}`, where `{N}` is the number of lambda parameters. They
only exist during compile-time and are changed to `Function{N+1}` and `SuspendFunction`. Because `SuspendFunction{N}` is not present at
runtime, there would be no way to distinguish ordinary functional type
from suspend functional type if we would not use `SuspendFunction` marker interface. To be more specific, it is used in
`is SuspendFunction{N}` and `as SuspendFunction{N}` expressions. For example, if we have code like
```kotlin
fun main() {
    val lambda: suspend () -> Unit = {}
    val a: Any = lambda
    print(a is (suspend () -> Unit))
}
```
for `is` expression we generate something like
```kotlin
a is SuspendFunction and TypeIntrinsics.isFunctionOfArity(a, 1)
```
That, by the way, is the reason why `SuspendLambda`'s constructor accepts arity.

Of course, all generated suspend lambdas implement `SuspendFunction` through `SuspendLambda` and `RestrcitedSuspendLambda`. Callable
references to suspend functions implement the interface directly.

### Suspend Lambda Layout
The ideal suspend lambda layout is the following:
1. supertypes: `kotlin/coroutines/jvm/internal/SuspendLambda` and `kotlin/jvm/functions/Function{N}`
2. package-private captured variables
3. private label field of int. Private, since it is used only in the lambda itself.
4. private fields for spilled variables. Same.
5. public final method `invokeSuspend` of type `(Ljava/lang/Object;)Ljava/lang/Object;`.
6. overrides `BaseContinuationImpl`'s `invokeSuspend`.
7. public final `create` of type `(<params>,Lkotlin/coroutines/Continuation)Lkotlin/coroutines/Continuation`.
`<params>` types are erased. In other words, their types are `Ljava/lang/Object;` as long as the number of parameters
is either 0 or 1. That is because the method overrides the base class's `create`.
8. public final `invoke` of type `(<params>,Ljava/lang/Object;)Ljava/lang/Object;`. Since it overrides `Function{N}`'s
`invoke`, types of `<params>` are erased.
9. public or package-private constructor: `<init>` of type `(<captured-variables>,Lkotlin/coroutines/Continuation;)V`,
where we call `SuspendLambda`'s constructor with arity and completion and initialize captured variables.
The compiler knows the arity, but the completion is provided as an argument to the constructor.

### kotlin.suspend
`suspend` soft keyword cannot be used with lambdas or function expression yet. It is not supported in the parser. However, writing the type
of the variable is quite annoying, so, since 1.3, there is a function `kotlin.suspend`, which can precede lambda without parameters and turn
it into suspend one. Since `suspend` is a soft keyword, it is possible to name a function `suspend`. A user can define a function named
`suspend`, which accepts lambdas, but the token sequence `suspend {` can only be used with `kotlin.suspend`. That is just for the transition
period and while suspend lambdas and function expressions are not supported in the parser.

FIXME: Support it in the parser and stop using the hack.

### Tail-Call Suspend Lambdas
FIXME: This feature is not implemented yet. Ideally, they should behave like callable references to suspend functions.
Meaning, they should
1. not have `create`, `invokeSuspend`, and all the fields, except for captured variables. Only constructor and `invoke`.
2. not inherit `BaseContinuationImpl` or any of its children.

## Suspend Functions
As explained in the continuation-passing style section, every suspending function's signature is changed: the compiler adds a continuation
parameter and changes the return type to `Any?`.

The tricky part becomes when we try to make it suspendable, in other words, when we build a state machine and generate a continuation.
Unlike suspend lambdas, we cannot reuse an existing class for the continuation, since the suspend function can be static, or there can be
several functions inside one class.

One way we can solve the issue is to turn the suspend function into a suspend lambda somewhat. We could generate a suspend lambda with code
of the suspend function and inside the function call the lambda. For example, when we have a function
like
```kotlin
suspend fun test() {
    suspendMe()
    suspendMe()
}
```
we could generate code like
```kotlin
val test$1: suspend () -> Unit = {
    suspendMe()
    suspendMe()
}

suspend fun test() {
    test$1()
}
```
As one can see, these two pieces of code are semantically identical. That, by the way, is how JS and Native back-ends generate suspending
functions. Furthermore, in JVM, we also used to do this, but not anymore.

The reason why in JVM we do not do this anymore is stack-traces. If we did copy the body of the suspend function to the lambda, the
stack-trace would look like
```text
suspendFun1$1.invokeSuspend
suspendFun2$1.invokeSuspend
suspendFun3$1.invokeSuspend
suspendFun4$1.invokeSuspend
suspendFun5$1.invokeSuspend
```
but we want it to look like
```text
suspendFun1
suspendFun2
suspendFun3
suspendFun4
suspendFun5
```
thus, instead of moving the function body to lambda, we keep it in the function and build the state-machine there. However, we also keep the
'lambda', so we store all spilled variables there and the label. This 'lambda' is called continuation, and it is, essentially, the state of
the coroutine. Unlike suspend lambdas, we split the state (and call it continuation) and the state-machine for suspending functions.

### Start
Nevertheless, there is another problem. To properly support the completion chain, we need to create the continuation and store the
continuation parameter in the `completion` field. We also need to support resuming the coroutine, i.e., we need to get `label` and spilled
variables from the continuation. So, we need to distinguish these two cases: starting anew and continuing previously suspended execution.
The easiest way to do this is to check for the type of continuation parameter. So, the function preamble will look like:
```kotlin
fun test($completion: Continuation<Unit>): Any? {
    val $continuation =
        if ($completion is test$1) $completion
        else test$1($completion)
    // state machine
}
```
As long as we generate distinct continuation types for each suspending function, the trick with the check allows us to distinguish these
two cases.

However, we have a third case: recursion. When we recursively call the function, the type of the continuation parameter is the same, as if
we just resumed (see next section for this). So, there three possible calls of the function:
1. direct call from another suspend function or suspend lambda
2. resumption
3. recursion

So, we need to store at least one another bit of information. We use sign bit of `label` field for this. Thus, the prefix of the function
looks like
```kotlin
fun test($completion: Continuation<Unit>): Any? {
    val $continuation =
        if ($completion is test$1 && $completion.label < 0) {
            $completion.label.sign_bit = 0
            $completion
        } else test$1($completion)
    // state machine
}
```
here, we assume that the sign bit is unset in recursive calls, while the continuation class sets it during the resume process. So, let us
see how we resume and set the bit.

### Resume
As we dealt with starting a suspend function and creating a coroutine (in a broad sense), we can tackle the resume process. As explained
earlier, when a coroutine (in a narrow sense) suspends, it returns `COROUTINE_SUSPENDED`. Thus, among the three essential processes of
coroutines: creation, suspension, and resumption, there is only the latter left.

In `BaseContinuationImpl.resumeWith` we call `invokeSuspend`. So, inside of `invokeSuspend`, we call the function and pass `this` as the
continuation parameter:
```kotlin
fun invokeSuspend(result: Result<Any?>): Any? {
    test(this)
}
```
However, we need to set sign bit of the label as well:
```kotlin
fun invokeSuspend(result: Result<Any?>): Any? {
    this.label.sign_bit = 1
    test(this)
}
```

Let us change the function to call another function that returns a value:
```kotlin
val c: Continuation<Int>? = null

fun suspendInt(): Int = suspendCoroutine {
    c = it
}

suspend fun test() {
    val i = suspendInt()
    println(i)
}

fun main() {
    builder {
        test()
    }
    c?.resume(42)
}
```
When we run the example, we get 42 printed. Meaning, that the result is somehow passed to the function. The only place we can pass it is
`invokeSuspend`. Also, there is only the continuation parameter of the function. Thus, we need to put the result to the continuation object
itself:
```kotlin
fun invokeSuspend(result: Result<Any?>): Any? {
    this.result = result
    this.label.sign_bit = 1
    test(this)
}
```
then, get the result from the continuation object in the function:
```kotlin
fun test($completion: Continuation<Unit>): Any? {
    val $continuation =
        if ($completion is test$1 && $completion.label < 0) {
            $completion.label.sign_bit = 0
            $completion
        } else test$1($completion)
    val $result = $continuation.result
    // state machine
}
```

Variables spilling is the same regardless, whether it is a lambda or a  function. However, we spill the variables to the continuation object.

### JVM: Parameters
Let us now have a look into how we deal with suspend function parameters. We do not generate fields for them, since a lambda uses them just
to pass the arguments from `invoke` to `invokeSuspend`. We do not need them for suspending functions: the arguments are locals; thus, we
reuse local variables spilling for them.

Nevertheless, keeping the parameters in the function signature breaks resumption. There is not enough information in continuation's
`invokeSuspend` to pass them as they were before or as they are now. So, we just put nulls for reference types and zeroes for primitives.
That means we cannot generate nullability checks at the beginning of the function, so they must be generated at the beginning of the first
state, to which we cannot resume.

For example, if we change the `test` function to accept an argument:
```kotlin
suspend fun dummy() {}

suspend fun test(a: Int) {
    dummy()
    dummy()
}
```
its continuation's `invokeSuspend` becomes something like
```kotlin
fun invokeSuspend(result: Result<Any?>): Any? {
    this.result = result
    this.label.sign_bit = 1
    test(0, this)
}
```

### JVM: Layout
We can now deduce the layout of the suspend function's continuation.

The ideal suspend lambda layout is the following:
1. supertypes: `kotlin/coroutines/jvm/internal/ContinuationImpl`
2. package-local label field of int. Package-local, considering the function uses it, and the function is outside of the class.
3. package-local fields for spilled variables. Same.
4. public final method `invokeSuspend` of type `(Ljava/lang/Object;)Ljava/lang/Object;`. It overrides `BaseContinuationImpl`'s
`invokeSuspend`, which calls the function.
5. public or package-private constructor: `<init>` of type `(Lkotlin/coroutines/Continuation;)V`, which calls
`BaseContinuatonImpl`.

### Local Suspend Functions
Local functions are weird: the way the compiler generates them is different in back-ends. Local suspend functions are even stranger.

#### Old JVM: Unerased Suspend Lambdas
Old JVM back-end generates local functions as lambdas. Thus, suspend local functions are generated as suspend lambda:
```kotlin
fun main() {
    suspend fun local(i: Int) {}
}
```
is generated as something like:
```kotlin
fun main() {
    val local: suspend (Int) -> Unit = {}
}
```
Doing so allows us to reuse the logic of captured variables and simplify the logic of code generation. However, because of the limitations
of old BE, its `create` and `invoke` are unerased. In other words, the compiler duplicates them, generating unerased and erased copies.
Unerased copy accepts typed parameters, and it contains the logic of lambda's `invoke` or `create`. The other accepts only `Any?`
parameters since they override supertype's functions, and delegates call to an unerased copy. Thus, the layout of local suspend functions is:
1. supertypes: `kotlin/coroutines/jvm/internal/SuspendLambda` and `kotlin/jvm/functions/Function{N}`
2. package-private captured variables
3. private label field of int. Private, since it is used only in the lambda itself.
4. private parameter fields. The reason for visibility is the same as for the `label` field.
5. private fields for spilled variables. Same.
6. public final method `invokeSuspend` of type `(Ljava/lang/Object;)Ljava/lang/Object;`.
It overrides `BaseContinuationImpl`'s `invokeSuspend`.
7. public final `create` of type `(<params>,Lkotlin/coroutines/Continuation)Lkotlin/coroutines/Continuation`.
`<params>` types are erased.
8. public final `create` of type `(<params>,Lkotlin/coroutines/Continuation)Lkotlin/coroutines/Continuation`.
`<params>` types are unerased.
9. public final `invoke` of type `(<params>,Ljava/lang/Object;)Ljava/lang/Object;`. `<params>` are erased.
10. public final `invoke` of type `(<params>,Lkotlin/coroutines/Continuation;)Ljava/lang/Object;`. `<params>` are unerased.
11. public or package-private constructor: `<init>` of type `(<captured-variables>,Lkotlin/coroutines/Continuation;)V`,
where we call `SuspendLambda`'s constructor with arity and completion and initialize captured variables.
As for suspending lambdas, the compiler knows the function's arity, but the completion is provided as an argument to the constructor.

FIXME: There is a massive amount of bugs because of this implementation. For example, local suspend functions can hardly be recursive.

#### JVM_IR: Static Functions
On the other hand, JVM_IR generates local functions as static functions with captured variables put as first parameters. Thus, suspend local
functions are generated as static functions as well. That reduces code size and method count and enables tail-call optimization.

An example:
```kotlin
fun main() {
    val aa: Long = 1
    suspend fun local(i: Int) {
        println(aa)
    }
}
```
is generated as something like
```kotlin
suspend fun main$1(aa: Long, i: Int) {
    println(aa)
}

fun main() {
    val aa: Long = 1
}
```

### Tail-Call Optimization
One might have noticed that we do not always need a state machine. For example, when a suspend function does not call another suspend
functions at all. Since every suspend call creates a continuation, it becomes quite expensive to have one in a loop. For these two reasons, we do
not generate a continuation class and a state machine for suspending functions, which have all their suspend calls in tail position. Since
there is no way they can suspend in the middle of the function, they do not need either of them: they have only one state.

Example of tail-call functions:
```kotlin
suspend fun returnsInt() = suspendCoroutine<Int> { it.resume(42) }

suspend fun tailCall1(): Int {
    return returnsInt()
}

suspend fun tailCall2() = returnsInt()
```

For both of the functions the compiler generates the following bytecode (before `CoroutineTransformerMethodVisitor`:
```text
INVOKESTATIC InlineMarker.beforeInlineCall
ALOAD 1 // continuation
ICONST 0 // before suspend marker
INVOKESTATIC InlineMarker.mark
INVOKESTATIC returnsInt()
ICONST 1 // after suspend marker
INVOKESTATIC InlineMarker.mark
INVOKESTATIC InlineMarker.afterInlineCall
ARETURN
```

After tail-call optimization, the code becomes
```text
ALOAD 1 // continuation
INVOKESTATIC returnsInt
ARETURN
```

The check whether the function is tail-call is simple: check, that all (reachable) suspension points are
1. not inside try-catch block
2. immediately followed by ARETURN with optional branching or stack modification, with one notable exception: `GETSTATIC Unit; ARETURN`
(more on that later).

`MethodNodeExaminer` contains the logic of the check. Since we use the same state machine builder in both back-ends (because we should
support bytecode inlining in JVM_IR), the logic is shared.

Note that because we do not create a state-machine, there is no reason to spill the variables, and thus we do not create a continuation
class. So, the completion chain will miss a link:
```kotlin
suspend fun returnsInt1() = suspendCoroutine<Int> { it.resume(42) }

suspend fun returnsInt2(): Int {
    val result = returnsInt1()
    println(result)
    return result
}

suspend fun returnsInt3() = returnsInt2()

suspend fun main() {
    println(returnsInt3())
}
```

If there were no tail-call optimization, the completion chain would look like:
```text
          null<------+
                     |
      +-----------+  |
   +->+returnsInt1|  |
   |  +-----------+  |
   |  |completion +--+
   |  +-----------+
   |
   |
   |  +-----------+
   |  |returnsInt2+<-+
   |  +-----------+  |
   +--+completion |  |
      +-----------+  |
                     |
      +-----------+  |
   +->+returnsInt3|  |
   |  +-----------+  |
   |  |completion +--+
   |  +-----------+
   |
   |
   |  +-----------+
   |  |    main   |
   |  +-----------+
   +--+completion |
      +-----------+
```

but with tail-call optimization, it becomes

```text
   +----->null
   |
   |  +-----------+
   |  |returnsInt2+<-+
   |  +-----------+  |
   +--+completion |  |
      +-----------+  |
                     |
      +-----------+  |
      |    main   |  |
      +-----------+  |
      |completion +--+
      +-----------+
```

`returnInt1` and `returnInt3` are tail-call and have no continuation.

In Old JVM back-end, local suspend functions are lambdas, they do not support tail-call optimization, but local suspend functions,
generated by JVM_IR do.

#### Redundant Locals Elimination
As explained in the section about variables spilling, the inliner spills stack before inlining and unspills it after the inlining. That
results in a bunch of repeated ASTORE and ALOAD instructions, which can break tail-call elimination since there can be a sequence of
`ASTORE; ALOAD` between the suspension point and ARETURN. This bytecode modification simplifies the chains and enables tail-call
optimization for these cases.

#### Tail-Call Optimization for Functions Returning Unit
There are some challenges if we want to make suspending functions, returning `Unit` tail-call. Let us have a look at one of them. If the
function returns `Unit`, `return` keyword is optional:
```kotlin
suspend fun returnsUnit() = suspendCoroutine<Unit> { it.resume(Unit) }

suspend fun tailCall1() {
    return returnsUnit()
}

suspend fun tailCall2() = returnsUnit()

suspend fun tailCall3() {
    returnsUnit()
}
```
in this example, `tailCall1` and `tailCall2` are covered by usual tail-call optimization. However, the last function is different. The
codegen generates the following bytecode:
```text
INVOKESTATIC InlineMarker.beforeInlineCall
ALOAD 1 // continuation
ICONST 0 // before suspending marker
INVOKESTATIC InlineMarker.mark(I)V
INVOKESTATIC returnsUnit()
ICONST 1 // after suspending marker
INVOKESTATIC InlineMarker.mark(I)V
INVOKESTATIC InlineMarker.afterInlineCall()V
POP
GETSTATIC kotlin/Unit.INSTANCE
ARETURN
```
as one sees, `Unit` is `POP`ed, and then is pushed to the stack and returned. Unfortunately, we cannot just remove
`POP; GETSTATIC kotlin/Unit.INSTANCE`: if we replace `returnsUnit` with `returnsInt`, the bytecode is the same. Since inside
`CoroutineTransformerMethodVisitor` we do not have information about return types of suspending calls, we see all of them as just `Any?`, we
need to mark calls to functions, returning Unit, with a marker. The marker is similar to suspend markers, but with a different argument:
`ICONST_2`. So, full bytecode for the `tailCall3` function becomes
```text
INVOKESTATIC InlineMarker.beforeInlineCall
ALOAD 1 // continuation
ICONST 0 // before suspending marker
INVOKESTATIC InlineMarker.mark(I)V
INVOKESTATIC returnsUnit()
ICONST_2 // returns unit marker
INVOKESTATIC InlineMarker.mark(I)V
ICONST 1 // after suspending marker
INVOKESTATIC InlineMarker.mark(I)V
INVOKESTATIC InlineMarker.afterInlineCall
POP
GETSTATIC kotlin/Unit.INSTANCE
ARETURN
```

After tail-call optimization, it is, expectedly, without a state-machine:
```text
ALOAD 1 // continuation
INVOKESTATIC returnsUnit()
ARETURN
```

Let's replace `returnsUnit` with `returnsInt` for the moment:
```kotlin
suspend fun returnsInt() = suspendCoroutine<Int> { it.resume(42) }

suspend fun tailCall() {
    returnsInt()
}
```
as explained, one cannot simply remove `POP; GETSTATIC kotlin/Unit.INSTANCE`, since in such case function, returning `Unit` would return
`Int`. However, there can be only one state in the state-machine. Thus, we simply keep `POP; GETSTATIC kotlin/Unit.INSTANCE`:
```text
ALOAD 1 // continuation
INVOKESTATIC returnsInt()
POP
GETSTATIC kotlin/Unit.INSTANCE
ARETURN
```

Nevertheless, there is a problem. Since the completion chain misses a link, there can be cases when a suspend function returning `Unit`
appears to return non-`Unit` value:
```kotlin
import kotlin.coroutines.*

var c: Continuation<Int>? = null

suspend fun returnsInt(): Int = suspendCoroutine { c = it }

suspend fun returnUnit() {
    returnsInt()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun main() {
    builder {
        println(returnUnit())
    }

    c?.resume(42)
}
```
This example, just like the previous one, has a tail-call function returning `Unit` (`returnUnit`) calls a function, returning non-`Unit`,
(`returnsInt`). The compiler generates the following completion chain: 
```text
           null<-----+
                     |
      +-----------+  |
   +->+ builder$1 |  |
   |  +-----------+  |
   |  |completion +--+
   |  +-----------+
   |
   |
   |  +-----------+
   |  |   main$1  |
   |  +-----------+
   +--+completion |
      +-----------+
```
That is right; there is only one continuation, generated by the compiler: `main$1`. Moreover, it is passed to `returnsUnit`, then to
`returnsInt` and finally is stored in `c` variable.

Now, let us see, what the codegen generates in `main$1` lambda before it builds the state-machine:
```text
INVOKESTATIC InlineMarker.beforeInlineCall
ALOAD 1 // continuation
ICONST 0 // before suspending marker
INVOKESTATIC InlineMarker.mark(I)V
INVOKESTATIC returnsUnit()Ljava/lang/Object;
ICONST_2 // returns unit marker
INVOKESTATIC InlineMarker.mark(I)V
ICONST 1 // after suspending marker
INVOKESTATIC InlineMarker.mark(I)V
INVOKESTATIC InlineMarker.afterInlineCall
INVOKEVIRTUAL println(Ljava/lang/Object;)V
GETSTATIC kotlin/Unit.INSTANCE
ARETURN
```
I replaced inlined `println` with a call for clarity. After turning into a state-machine, the code becomes: 
```kotlin
fun invokeSuspend($result: Any?): Any? {
    when (this.label) {
        0 -> {
            this.label = 1
            $result = returnsUnit(this)
            if ($result == COROUTINE_SUSPENDED) return COROUTINE_SUSPENDED
            goto 1
        }
        1 -> {
            println($result)
            return Unit
        }
        else -> {
            throw IllegalStateException("call to 'resume' before 'invoke' with coroutine")
        }
    }
}
```
After we resume the coroutine (its `label` value is `1`), `$result` is `42`, and it will get printed. That is right, a function returning
`Unit` appears returning non-`Unit`. To fix the issue, we replace returns unit markers with `POP; GETSTATIC kotlin/Unit.INSTANCE` sequence.
That way, we ignore the value, passed to `resume` the same way as if there was no suspension. By the way, we do the same in `callSuspend` and
`callSuspendBy` functions.

However, we cannot always do the replacement, as shown in the following example:
```kotlin
import kotlin.coroutines.*

var c: Continuation<*>? = null

suspend fun <T> tx(lambda: () -> T): T = suspendCoroutine { c = it; lambda() }

object Dummy

interface Base<T> {
    suspend fun generic(): T
}

class Derived: Base<Unit> {
    override suspend fun generic() {
        tx { Dummy }
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object: Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>){
            result.getOrThrow()
        }
    })
}

fun main() {
    var res: Any? = null

    builder {
        val base: Base<*> = Derived()
        res = base.generic()
    }

    (c as? Continuation<Dummy>)?.resume(Dummy)

    println(res)
}
```
In this example, we cannot be sure that `generic` returns `Unit`. In this case, the compiler disables tail-call optimization. More
generally, the compiler disables tail-call optimization for functions returning `Unit` if the function overrides a function, returning
non-`Unit` type.

### Returning Inline Classes
Before 1.4, if a suspend function returns an inline class, the class's value is boxed. That is undesirable for inline classes containing
reference types since it leads to additional allocations. Thus, if the compiler can verify that callee returns an inline class, it does not
generate boxing instructions in the callee and unboxing instructions in the caller. Otherwise, the callee returns a boxed value, as in the
following example:
```kotlin
inline class IC(val a: Any)

interface I {
    suspend fun overrideMe(): Any
}

class C : I {
    override suspend fun overrideMe(): IC = IC("OK")
}

suspend fun main() {
    val i = C()
    println(i.overrideMe())
}
```
Here, the compiler cannot verify that the call-site always expects inline class. Thus, `overrideMe` always boxes the class.

However, the optimization is not as straightforward as it seems. There are two paths of the execution of a suspend call: direct when the
callee returns to the caller, and resume route when the callee returns to `invokeSuspend` and then to `resumeWith`, which calls
`completion.resumeWith`, which calls `invokeSuspend,` which calls the caller. In the direct path (the most common case), the class is
unboxed. 

However, in the resume path, we should box the inline class (in this case, we care less about performance).
`BaseContinuationImpl.resumeWith` calls `invokeSuspend`, and it expects that the return type of
`invokeSuspend` is "T | COROUTINE_SUSPENDED", where T is a boxed inline class in this case. Breaking this contract leads to throwing the
exception in the following example:
```kotlin
import kotlin.coroutines.*

fun main() {
    builder {
        signInFlowStepFirst()
    }
    continuation!!.resumeWithException(Exception("BOOYA"))
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object : Continuation<Unit> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<Unit>) {
            result.getOrThrow()
        }
    })
}

var continuation: Continuation<Unit>? = null

suspend fun suspendMe() = suspendCoroutine<Unit> { continuation = it }

@Suppress("RESULT_CLASS_IN_RETURN_TYPE")
suspend fun signInFlowStepFirst(): Result<Unit> = try {
    Result.success(suspendMe())
} catch (e: Exception) {
    Result.failure(e)
}
```
The explanation of the bug cause is not that simple:
1. `signInFlowStepFirst` call `suspendMe` and suspends
2. We resume the execution with an exception.
3. Inside `signInFlowStepFirst`, we wrap the exception with Result class, just like in a burrito. 
4. Since it is the resume path (we resumed the execution), the execution returns to `invokeSuspend`, which returns `Result$Failure` to
`BaseContinuationImpl.resumeWith`.
5. `BaseContinuationImpl.resumeWith` wraps `Result$Failure` with another `Result`, but since `Result` is an inline class, the result
(pun not intended) of the operation is the same `Result$Failure`.
6. `BaseContinuationImpl.resumeWith` calls `completion.resumeWith`, passing the `Result$Failure` as the argument, which is considered as
`resumeWithException` by the completion.

So. We need to box inline class inside `invokeSuspend` if the function returns inline class, and the compiler has optimized boxing, as well
as inside the callable reference. That fixes the coroutine contract of `invokeSuspend`.

However, in the direct path, generated code expects an unboxed value. So, in the resume path of the caller, we should unbox it. There are a
couple of places we can unbox it: `invokeSuspend` and unspilling inside a state-machine. Consider the following snippet:
```kotlin
import kotlin.coroutines.*

inline class IC(val a: Any)

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) { it.getOrThrow() })
}

var c: Continuation<Any>? = null

suspend fun returnsIC() = suspendCoroutine<IC> { c = it as Continuation<Any> }
suspend fun returnsAny() = suspendCoroutine<Any> { c = it }

suspend fun test() {
    println(returnsIC())
    println(returnsAny())
}

fun main() {
    builder {
        test()
    }
    c?.resume(IC("OK1"))
    c?.resume("OK2")
}
```
Here, we resume the `test` function twice, once with the inline class, the other with an ordinary one. However, we need to box the value
only once: during the first resumption. Meaning that we need to add complex logic to `invokeSuspend` if we want to box the value. It is
simpler to do the boxing inside the state-machine.

#### Unbox Inline Class Markers
The state-machine section explained how the compiler turns sequential code into a state-machine. In a couple of words, it generates markers
around suspension points. So, if we want to pass information from the codegen to the state-machine builder, or the inliner, some markers
are a go-to. Naturally, we generate markers for the unboxing sequence we need to generate at the resume path.

Consider the following example.
```kotlin
inline class IC(val s: String)

suspend fun ic() = IC("OK")

suspend fun main() {
    println(ic().s)
}
```
The codegen has to generate an unboxing sequence for the `IC` class in the `main` function, so the class builder moves it to the resume path
since `main` is not a tail-call function, and thus has a state-machine, unlike `ic`. Also, it should tell the builder that these
instructions are to move. Thus, it surrounds them with new markers, now with ids `8` and `9`.

```text
INVOKESTATIC ic(Lkolint/coroutines/Continuation;)Ljava/lang/Object;
BIPUSH 8
INVOKESTATIC kotlin/jvm/internal/InlineMarker(I)V
CHECKCAST LIC;
INVOKEVIRTUAL IC.unbox-impl()Ljava/lang/String;
CHECKCAST Ljava/lang/Object;
BIPUSH 9
INVOKESTATIC kotlin/jvm/internal/InlineMarker(I)V
```
Note that `CHECKCAST Ljava/lang/Object;` is a part of the closing marker. We need to generate it not to interfere with bytecode analysis.
Otherwise, bytecode analysis assumes that the suspend call's return type is `String`, not `Any?`. After moving the unboxing to the resume
path, the builder removes the cast.

This way of passing the information also applies to inlining. Consider the following example:
```kotlin
import kotlin.coroutines.*

// Library lib1
inline class IC(val a: Any)

var c: Continuation<Any>? = null

suspend fun returnsIC() = suspendCoroutine<IC> { c = it as Continuation<Any> }

// Library lib2 depends on lib2

suspend inline fun inlineMe() {
    println(returnsIC())
}

// Main module

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) { it.getOrThrow() })
}

suspend fun test() {
    inlineMe()
}

fun main() {
    builder {
        test()
    }
    c?.resume(IC("OK1"))
}
```
Here, `inlineMe$$forInline` has no state-machine, and thus, the direct path is similar to the resume path. However, the codegen generates
the markers and the inliner inlines the markes:
```text
ICONST_1
INVOKESTATIC kotlin.jvm.internal.InlineMarker.mark(I)V
// The suspension point
ICONST_2
INVOKESTATIC kotlin.jvm.internal.InlineMarker.mark(I)V
ICONST_8
INVOKESTATIC kotlin.jvm.internal.InlineMarker.mark(I)V
// After this marker, there should be a call to box-impl
INVOKEVIRTUAL IC.unbox-impl(Ljava/lang/Object;)LIC;
CHECKCAST Ljava/lang/Object;
BIPUSH 9
INVOKESTATIC kotlin/jvm/internal/InlineMarker(I)V
```
Finally, after the inlining, the state-machine builder moves the unboxing to the resume path.

## Inline
Inlining a suspend function is a tricky business.

Before we get further, let us explain that there are 64 possible combinations of inline functions with parameter:
1. The function can be either suspend or ordinary
2. A parameter can be noinline, inline or crossinline lambda; or no lambda at all
3. An argument can be a block, which we inline, or a variable, which we cannot inline
4. The parameter can be suspendable or ordinary
5. We can inline the function or call it via reflection

Some of them are ill-formed, of course, because there are no suspend non-functional parameters in Kotlin. Suspend parameters can have only
functional types. Also, we cannot inline variable when we call it via reflection.

Let us draw a table with all possible combinations, which are valid and fill it one row at a time:
```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|ordinary|noinline      |block        |ordinary      |inline   |no suspend call allowed                        |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|noinline      |block        |suspend       |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|noinline      |variable     |ordinary      |inline   |no suspend call allowed                        |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|noinline      |variable     |ordinary      |call     |no suspend call allowed                        |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|noinline      |variable     |suspend       |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|noinline      |variable     |suspend       |call     |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|inline        |block        |ordinary      |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|inline        |block        |suspend       |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|inline        |variable     |ordinary      |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|inline        |variable     |ordinary      |call     |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|inline        |variable     |suspend       |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|inline        |variable     |suspend       |call     |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|crossinine    |block        |ordinary      |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|crossinine    |block        |suspend       |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|crossinine    |variable     |ordinary      |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|crossinine    |variable     |ordinary      |call     |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|crossinine    |variable     |suspend       |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|crossinine    |variable     |suspend       |call     |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|no lambda     |variable     |ordinary      |inline   |no suspend call allowed                        |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|no lambda     |variable     |ordinary      |call     |no suspend call allowed                        |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |noinline      |block        |ordinary      |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |noinline      |block        |suspend       |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |noinline      |variable     |ordinary      |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |noinline      |variable     |ordinary      |call     |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |noinline      |variable     |suspend       |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |noinline      |variable     |suspend       |call     |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |inline        |block        |ordinary      |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |inline        |block        |suspend       |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |inline        |variable     |ordinary      |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |inline        |variable     |ordinary      |call     |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |inline        |variable     |suspend       |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |inline        |variable     |suspend       |call     |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |crossinine    |block        |ordinary      |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |crossinine    |block        |suspend       |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |crossinine    |variable     |ordinary      |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |crossinine    |variable     |ordinary      |call     |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |crossinine    |variable     |suspend       |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |crossinine    |variable     |suspend       |call     |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |no lambda     |variable     |ordinary      |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |no lambda     |variable     |ordinary      |call     |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

Note that not all ordinary functions with ordinary parameters are out of the scope of the document.

Let us explain what `block` and `variable` of `argument kind` mean. We can call an inline function with lambda parameter in a couple of
ways: by providing a block or passing a variable of lambda type:
```kotlin
inline fun inlineMe(c: () -> Unit) { c() }

fun main() {
    // block
    inlineMe { println("block") }

    // variable
    val variable = { println("block") }
    inlineMe(variable)
}
```

With this out of the way, let us now consider suspend inline functions with no lambda parameter.

### Inline Suspend Functions with Ordinary Parameters
```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|suspend |no lambda     |variable     |ordinary      |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |noinline      |block        |ordinary      |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |noinline      |variable     |ordinary      |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

Suspend functions should have a state-machine unless they are tail-call, and we cannot inline a state-machine into another state-machine,
since it will reset the label field.

We generate them as if they are ordinary functions, but they have all the markers already generated. In other words, if we have an inline
suspend function like:
```kotlin
suspend fun returnsUnit() = suspendCoroutine<Unit> { it.resume(Unit) }

suspend inline fun inlineMe() {
    returnsUnit()
}
```

The compiler generates the following bytecode:
```text
ALOAD 1 // continuation
ICONST 0 // before suspending marker
INVOKESTATIC InlineMarker.mark
INVOKESTATIC returnsUnit()
ICONST_2 // returns unit marker
INVOKESTATIC InlineMarker.mark
ICONST 1 // after suspending marker
INVOKESTATIC InlineMarker.mark
POP
GETSTATIC kotlin/Unit.INSTANCE
ARETURN
```
if one looks closely, this is exactly the bytecode that would come to `CoroutineTransformerMethodVisitor` if we would not have declared the
function inline, but without stack spilling markers, since the inliner spills the stack for us.

Now, let us have another inline function, which calls this one.
```kotlin
suspend inline fun inlineMe2() {
    inlineMe()
    inlineMe()
}
```
the generated bytecode would look like:
```text
ALOAD 1 // continuation
ICONST 0 // before suspending marker
INVOKESTATIC InlineMarker.mark
INVOKESTATIC returnsUnit()
ICONST_2 // returns unit marker
INVOKESTATIC InlineMarker.mark
ICONST 1 // after suspending marker
INVOKESTATIC InlineMarker.mark
POP

ALOAD 1 // continuation
ICONST 0 // before suspend marker
INVOKESTATIC InlineMarker.mark
INVOKESTATIC returnsUnit()
ICONST_2 // returns unit marker
INVOKESTATIC InlineMarker.mark
ICONST 1 // after suspend marker
INVOKESTATIC InlineMarker.mark
POP

GETSTATIC kotlin/Unit.INSTANCE
ARETURN
```
as one sees, it inlines the inline functions. However, it does not generate a state-machine since the function will be inlined later. Note
that the compiler does not generate suspending markers around inlined bytecode, since the state-machine should be flat, so nested suspension
points are not allowed.

When lambda parameters are ordinary, there are no markers around the parameter call.

Now we can fill the rows:
```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|suspend |no lambda     |variable     |ordinary      |inline   |no state-machine with markers                  |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |noinline      |block        |ordinary      |inline   |no state-machine with markers                  |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |noinline      |variable     |ordinary      |inline   |no state-machine with markers                  |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

### Java Interop and Reflection
```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|suspend |no lambda     |variable     |ordinary      |call     |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |noinline      |variable     |ordinary      |call     |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

However, it, of course, is not that simple. Coroutine inlining is notorious for its complexity, simply because of the number of corner cases
it needs to support. Backward interop with Java (calling Kotlin code from Java) is one of them. It should be possible to call inline
functions from Java. Since javac does not inline them, generated code calls them directly. Yes, even inline suspend function. The reason
for backward compatibility support is simple: reflection works the same as calling from Java. It does not inline the functions but calls
them. For inline suspend functions, it means that when we call the function using reflection or Java, it should have a state-machine.
However, on the other hand, we should not have it, since we cannot inline a state-machine into state-machine.

Thus, we cannot suffice by generating only one function. Instead, we generate two versions of the inline function: one of them is for
inliner, the other is to call directly and via reflection. The name of the version for the direct call is left unchanged and has a
state-machine and a continuation. The version for the inliner has no state-machine but has the suspend
calls marked and name with `$$forInline` suffix.

For example, for code like
```kotlin
suspend fun returnsUnit() = suspendCoroutine<Unit> { it.resume(Unit) }

suspend inline fun inlineMe() {
    returnsUnit()
}

suspend inline fun inlineMe2() {
    inlineMe()
    inlineMe()
}
```
the compiler generates the following methods:
```text
// Can be called directly, have state-machine, unless tail-call
public returnsUnit(L/kotlin/coroutines/Continuation;)Ljava/lang/Object;
public inlineMe(L/kotlin/coroutines/Continuation;)Ljava/lang/Object;
public inlineMe2(L/kotlin/coroutines/Continuation;)Ljava/lang/Object;
// For inliner use only, no state-machine, suspend calls are marked
private inlineMe$$forInline(L/kotlin/coroutines/Continuation;)Ljava/lang/Object;
private inlineMe2$$forInline(L/kotlin/coroutines/Continuation;)Ljava/lang/Object;
```
Note that `$$forInline` versions have private visibility, so tools like proguard can easily remove them. This whole function duplication is
to preserve the semantics of the code, no matter whether we inline the function or call it via reflection.

```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|suspend |no lambda     |variable     |ordinary      |call     |state-machine                                  |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |noinline      |variable     |ordinary      |call     |state-machine                                  |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

### Inline-Only Functions
One exception to the rule of two functions is inline-only functions. For example, if they are annotated with `@kotlin.internal.InlineOnly`
annotation or have reified type parameters. Since they cannot be called via reflection or from Java, we do not need to duplicate them. Thus,
there is only one version - for inliner. However, since there is no version for
reflection, we do not need to mangle these functions. In other words, we keep the name of the function. For example, if we have an
inline-only function:
```kotlin
suspend fun blackhole() {}

suspend inline fun <reified T> inlineMe(t: T): T {
    blackhole()
    return t
}
```
generated bytecode will look like:
```text
synthtic public inlineMe(Ljava/lang/Object;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
ALOAC 0 // parameter
ALOAD 1 // continuation
ICONST_0 // before suspend call marker
INVOKESTATIC kotlin/jvm/internal/InlineMarker.mark
INVOKESTATIC blackhole (Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
ICONST_2 // returns Unit marker
INVOKESTATIC kotlin/jvm/internal/InlineMarker.mark
ICONST_1 // after suspend call marker
INVOKESTATIC kotlin/jvm/internal/InlineMarker.mark
POP
ARETURN
```

### Ordinary Inline Functions
Since ordinary inline functions do not have a state-machine, they can be both called directly and inlined. Thus, there is no difference
between `call,` and `inline` `call kind`s for `ordinary` functions. That reduces the number of distinct combinations even further.

There is, however, one notable exception.

### Ordinary Inline Parameter Of Ordinary Inline Function
```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|ordinary|inline        |block        |ordinary      |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|inline        |variable     |ordinary      |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|inline        |variable     |ordinary      |call     |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

Suppose, we have code like
```kotlin
suspend fun isFoo(): Boolean = TODO()

suspend fun main() {
    println(listOf(1, 2, 3).filter { isFoo() })
}
```
As one sees, we use a suspend function inside ordinary inline lambda block argument of an ordinary inline function. The inliner inlines the
lambda into the function, and then the function is inlined into call-site, which is a suspend context. Thus, we should support it. This way,
we can retrieve continuation from the context to pass it to the call. Note that if either the function or the argument is not inlined, there
would be no continuation in the context. Thus we forbid the cases.

```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|ordinary|inline        |block        |ordinary      |inline   |suspend calls in block allowed                 |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|inline        |variable     |ordinary      |inline   |no suspend calls allowed                       |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|inline        |variable     |ordinary      |call     |no suspend calls allowed                       |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

### Ordinary Inline Parameter of Suspend Inline Function
```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|suspend |inline        |block        |ordinary      |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |inline        |variable     |ordinary      |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |inline        |variable     |ordinary      |call     |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

Since the lambda is inlined, there is a continuation in context. Thus, we can call suspend functions. Also, this is a suspend inline
function. Thus we duplicate it.

```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|suspend |inline        |block        |ordinary      |inline   |suspend calls in block allowed,                |
|        |              |             |              |         |markers without state-machine                  |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |inline        |variable     |ordinary      |inline   |no suspend call in argument allowed,           |
|        |              |             |              |         |markers without state-machine                  |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |inline        |variable     |ordinary      |call     |no suspend call in argument allowed,           |
|        |              |             |              |         |state-machine                                  |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

### Suspend Inline Parameter of Suspend Inline Function
```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|suspend |inline        |block        |suspend       |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |inline        |variable     |suspend       |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |inline        |variable     |suspend       |call     |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

If the parameter is inlined into the function, the lambda's calls use the function's continuation. Thus, it is not necessary to define it
as suspend. So, we generate a warning for a suspend inline functions with inline suspending lambda parameters, asking the programmer to
remove `suspend` keyword. The programmer is free to ignore the warning in other cases. Since the most common case is inlining, the warning
makes sense, and we decided to keep it when we discovered that there are cases of no inlining.

Suspension points cannot be nested, so, we generate different suspend markers: inline suspend markers. They are either removed by the
inliner, if it inlines the lambda, or replaced by real suspend markers when there is no lambda inlining.

```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|suspend |inline        |block        |suspend       |inline   |WARNING to remove `suspend` keyword            |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |inline        |variable     |suspend       |inline   |WARNING should be ignored,                     |
|        |              |             |              |         |no state-machine with markers                  |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |inline        |variable     |suspend       |call     |WARNING should be ignored, state-machine       |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

### Inline Suspend Parameter of Ordinary Inline Function
```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|ordinary|inline        |block        |suspend       |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|inline        |variable     |suspend       |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|inline        |variable     |suspend       |call     |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

Since the suspend lambda parameter's body should be inlined into the body of an ordinary inline function, which, in turn, can be inlined
into a non-suspend context, there is no way we can retrieve continuation to pass to suspend calls, which the lambda could have. In other
words, we forbid this combination.

```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|ordinary|inline        |block        |suspend       |inline   |FORBIDDEN                                      |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|inline        |variable     |suspend       |inline   |FORBIDDEN                                      |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|inline        |variable     |suspend       |call     |FORBIDDEN                                      |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

### Noinline Suspend Parameter of Suspend Inline Function
```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|suspend |noinline      |block        |suspend       |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |noinline      |variable     |suspend       |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |noinline      |variable     |suspend       |call     |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

Before we get to inline suspend lambdas, or crossinline suspend lambdas,  let us see how noinline suspend lambda parameters of inline
functions work. For example, if we have even the simplest function possible:
```kotlin
suspend inline fun inlineMe(noinline c: suspend () -> Unit) { c() }
```
we need to wrap the call of the parameter with suspending markers; otherwise, the call will not have a state in the state-machine. Thus,
the compiler generates code like
```text
ALOAD 0 // lambda parameter
ALOAD 1 // continuation
ICONST_0 // before suspending call marker
INVOKESTATIC kotlin/jvm/internal/InlineMarker.mark
INVOKEINTERFACE Function1.invoke (Ljava/lang/Object;)Ljava/lang/Object;
ICONST_2 // returns Unit marker
INVOKESTATIC kotlin/jvm/internal/InlineMarker.mark
ICONST_1 // after suspending call marker
INVOKESTATIC kotlin/jvm/internal/InlineMarker.mark
POP
GETSTATIC kotlin/Unit.INSTANCE
ARETURN
```

Of course, for `call`, we also generate a state-machine.

Note, that we can call `noinline` parameter inside inner lambda or object, like
```kotlin
suspend inline fun inlineMe(noinline c: suspend () -> Unit) = suspend { c() }
```
in this case, we just generate a state-machine for the lambda, and the inliner copies it when it inlines the function.

```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|suspend |noinline      |block        |suspend       |inline   |if called in function, markers around invoke   |
|        |              |             |              |         |if called in inner function, state-machine     |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |noinline      |variable     |suspend       |inline   |if called in function, markers around invoke   |
|        |              |             |              |         |if called in inner function, state-machine     |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |noinline      |variable     |suspend       |call     |state-machine with parameter invoke in a state |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

### Noinline Suspend Parameter of Ordinary Inline Function
```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|ordinary|noinline      |block        |suspend       |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|noinline      |variable     |suspend       |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|noinline      |variable     |suspend       |call     |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

In order to call suspend noinline parameter, we need a suspend context. The easiest way to do this is to wrap the call in a suspend lambda:
```kotlin
inline fun inlineMe(noinline c: suspend () -> Unit) = suspend { c() }
```

The result is the same as in the previous section: the call is inside a separate state, but this time it is not inside the function itself,
it is inside the lambda. Since this is an ordinary inline function, it has no state-machine, the lambda does. The inliner copies its class
when it inlines the function and leaves the state-machine. This way, either we both inline the function and call it, semantics are the same.

```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|ordinary|noinline      |block        |suspend       |inline   |state-machine with parameter invoke in a state |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|noinline      |variable     |suspend       |inline   |state-machine with parameter invoke in a state |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|noinline      |variable     |suspend       |call     |state-machine with parameter invoke in a state |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

### Crossroutines
Finally, the trickiest part of coroutines codegen: crossinline lambdas captured inside suspend functions or lambdas, or `crossroutines`.

Let us begin with a simple example:
```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|ordinary|crossinine    |block        |suspend       |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

```kotlin
interface SuspendRunnable {
    suspend fun run()
}

suspend fun dummy() {}

inline fun crossinlineMe(crossinline c: suspend () -> Unit): SuspendRunnable =
    object : SuspendRunnable {
        override suspend fun run() {
            dummy()
            c()
        }
    }
```

The compiler generated code for the inline function will look like
```kotlin
public final class crossilineMe$1($captured_local_variable$0: Function1<Continuation<Unit>, Unit>) {
    final /*package-local*/ $c : Function1<Continuation<Unit>, Unit>

    init {
        $c = $captured_local_variable$0
    }

    public override fun run($completion: Continuation) {
        beforeFakeContinuationConstructionCall()
        run$1()
        afterFakeContinuationConstructionCall()
        dummy()
        this.$c.invoke($completion)
    }

    public final class run$1 {
        // Partially built continuation.
    }
}

public final fun crossinlineMe(c: Function1<Continuation<Unit>, Unit>): SuspendRunnable {
    return crossinlineMe$1(c)
}
```
When the inliner inlines the `crossinlineMe` function, it first inlines its lambda into the object in the process, which is called anonymous
object transformation, replaces the usages of the object, and finally inlines the function.

Note `run$1` class. It is `fake continuation`, continuation class, generated for the inliner by codegen. If it were not present, the inliner
would need to generate the continuation class. However, with fake continuation, it just needs to transform fake continuation, like it
transforms any other inner object. Fake continuation is simply a continuation class without spilled variables.

For example, if we use the function, like:
```kotlin
suspend fun main() {
    crossinlineMe {
        pritln("OK")
    }.run()
}
```
Firstly, we transform the object:
```kotlin
public final class main$1 {
    public override fun run($completion: Continuation) {
        val $continuation = if ($completion is run$1 && $completion.label.sign_bit.is_set) {
            $completion.label.sign_bit.unset
            $completion
        } else {
            run$1($completion)
        }
        when ($continuation.label) {
            0 -> {
                dummy()
                // error handling is omitted
            }
            1 -> {
                println("OK")
                return
            }
            else -> {
                error(...)
            }
        }
    }

    public final run$1: Continuation {
        // Continuation's content
    }
}
```
as one sees, we generate state-machine during the transformation, and the inliner transforms the fake continuation. The state-machine
builder removes the fake continuation constructor call. To do this, it looks for before and after fake continuation constructor call
markers, which have values `4` and `5` respectively.

Then we replace the object usages:
```kotlin
public final fun tmp(): SuspendRunnable {
    return main$1()
}
```
and finally, we inline the function:
```kotlin
public final fun main$$$($completion: Continuation<Unit>) {
    main$1().run()
}
```

Since we inline the lambda into the `run` function, we cannot generate the state-machine for it during codegen, only during the
transformation.

Because the parameter is declared as `suspend`, and the function itself is not, we cannot call the parameter inside the function; we need
to have some suspend context, i.e., inner lambda or inner object with a suspend function.

Note, if the lambda does not contain suspending calls, we should not put the inlined lambda into a separate state; in other words, we
should remove suspend markers around the inlined code. To do this, we introduced new suspend markers: before and after inline suspend call
markers. Their values are `6` and `7`, respectively. If the lambda is inlined, after lambda inlining, before building the state-machine,
the inliner removes the markers. Otherwise, it replaces the inline suspend markers with real suspend markers if the argument is not inlined.

```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|ordinary|crossinine    |block        |suspend       |inline   |inline suspend markers around invoke           |
|        |              |             |              |         |fake continuation for inliner to transform     |
|        |              |             |              |         |state-machine is built after inlining          |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

Let us now assume that we do not inline the parameter:
```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|ordinary|crossinine    |variable     |suspend       |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|crossinine    |variable     |suspend       |call     |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```
We cannot call the function, since it has no state-machine inside the object's suspend function. This dilemma of needing the state-machine
and needing not to have one is the same as inline suspend functions. Thus the solution is also the same: we duplicate the suspend function,
in this case, `run`:
```kotlin
public final class crossilineMe$1($captured_local_variable$0: Function1<Continuation<Unit>, Unit>) {
    final /*package-local*/ $c : Function1<Continuation<Unit>, Unit>

    init {
        $c = $captured_local_variable$0
    }

    public override fun run$$forInline($completion: Continuation) {
        beforeFakeContinuationConstructionCall()
        run$1()
        afterFakeContinuationConstructionCall()
        dummy()
        this.$c.invoke($completion)
    }

    public override fun run($completion: Continuation) {
        val $continuation = if ($completion is run$1 && $completion.label.sign_bit.is_set) {
            $completion.label.sign_bit.unset
            $completion
        } else {
            run$1($completion)
        }
        when ($continuation.label) {
            0 -> {
                dummy()
                // error handling is omitted
            }
            1 -> {
                println("OK")
                return
            }
            else -> {
                error(...)
            }
        }
    }

    public final run$1: Continuation {
        // Continuation's content
    }
}

public final fun crossinlineMe(c: Function1<Continuation<Unit>, Unit>): SuspendRunnable {
    return crossinlineMe$1(c)
}
```
FIXME: we do not need fake continuation anymore unless the function is tail-call or inline-only. The inliner transforms `run`'s continuation
instead. It can retrieve the continuation from the `run`'s function header.

As one sees, we have both versions: one with a state-machine, which the inliner throws away when it transforms the object, and the other is
for inlining, which is used as a template to generate 'real' state-machine.

In the case of suspending lambdas, we duplicate its `invokeSuspend` method.

```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|ordinary|crossinine    |variable     |suspend       |inline   |suspend function duplication                   |
|        |              |             |              |         |inline suspend markers replaced with real ones |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|crossinine    |variable     |suspend       |call     |suspend function duplication                   |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

Let's now see, what happens, when we inline the function into another inline function:
```kotlin
inline fun crossinlineMe2(crossinline c: suspend () -> Unit): SuspendRunnable =
    object : SuspendRunnable {
        override suspend fun run() {
            crossinlineMe { c() }
        }
    }
```
In this case, we cannot just throw away the state-machine template (a version of the function passed to the state-machine builder). So, if
the inliner inlines the function into another inline function, it keeps the template (after inlining, of course).
```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|ordinary|crossinine    |block        |suspend       |inline   |inline suspend markers around invoke           |
|        |              |             |              |         |if inline-site is inline, keep the template    |
|        |              |             |              |         |fake continuation for inliner to transform     |
|        |              |             |              |         |state-machine is built after inlining          |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

Let us now make the function itself suspend:

```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|suspend |crossinine    |block        |suspend       |inline   |                                               |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

```kotlin
suspend inline fun crossinlineMe(crossinline c: suspend () -> Unit): SuspendRunnable =
    object : SuspendRunnable {
        override suspend fun run() {
            c()
        }
    }
```
The codegen, of course, duplicates it, but it does not duplicate the object. So, it is merely a combination of inline suspend and
crossinline suspend.

There is one difference. You see, like `noinline` parameters, `crossinline` can be called in the function itself:
```kotlin
suspend inline fun inlineMe(crossinline c: suspend () -> Unit) {
    c()
}
```
In this example, `inlineMe`'s parameter, albeit being declared as `crossinline`, is inline. By the way, this is how parameters of
`suspendCoroutineUninterceptedOrReturn` and `suspendCoroutine` behave.

Since the parameter is inline, the behavior is the same, as there was no `crossinline` keyword, but without warning of redundant suspend
modifier.

```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|suspend |crossinine    |block        |suspend       |inline   |inline suspend markers around invoke           |
|        |              |             |              |         |if inline-site is inline, keep the template    |
|        |              |             |              |         |fake continuation for inliner to transform     |
|        |              |             |              |         |state-machine is built after inlining          |
|        |              |             |              |         |function is duplicated                         |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

To streamline the process of a suspend function transformation, since it is already tricky enough, we do not support ordinary crossinline
parameters in different ways. If its call-site is suspending, we duplicate it and then transform the object.

```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|ordinary|crossinine    |variable     |ordinary      |inline   |if lambda call-site is suspend, duplicate it   |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |crossinine    |block        |ordinary      |inline   |if lambda call-site is suspend, duplicate it   |
|        |              |             |              |         |function is duplicated                         |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |crossinine    |variable     |ordinary      |inline   |if lambda call-site is suspend, duplicate it   |
|        |              |             |              |         |function is duplicated                         |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |crossinine    |variable     |ordinary      |call     |if lambda call-site is suspend, duplicate it   |
|        |              |             |              |         |function is duplicated                         |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

### Summary
Here is the full list of all possible combinations, excluding invalid ones. Now one can see why coroutines inlining is a tricky business.

```text
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|function|parameter kind|argument kind|parameter type|call kind|notes                                          |
+========+==============+=============+==============+=========+===============================================+
|ordinary|noinline      |block        |ordinary      |inline   |no suspend call allowed                        |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|noinline      |block        |suspend       |inline   |state-machine with parameter invoke in a state |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|noinline      |variable     |ordinary      |inline   |no suspend call allowed                        |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|noinline      |variable     |ordinary      |call     |no suspend call allowed                        |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|noinline      |variable     |suspend       |inline   |state-machine with parameter invoke in a state |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|noinline      |variable     |suspend       |call     |state-machine with parameter invoke in a state |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|inline        |block        |ordinary      |inline   |suspend calls in block allowed                 |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|inline        |block        |suspend       |inline   |FORBIDDEN                                      |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|inline        |variable     |ordinary      |inline   |no suspend calls allowed                       |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|inline        |variable     |ordinary      |call     |no suspend calls allowed                       |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|inline        |variable     |suspend       |inline   |FORBIDDEN                                      |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|inline        |variable     |suspend       |call     |FORBIDDEN                                      |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|crossinine    |block        |ordinary      |inline   |no suspend call allowed                        |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|crossinine    |block        |suspend       |inline   |inline suspend markers around invoke           |
|        |              |             |              |         |if inline-site is inline, keep the template    |
|        |              |             |              |         |fake continuation for inliner to transform     |
|        |              |             |              |         |state-machine is built after inlining          |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|crossinine    |variable     |ordinary      |inline   |if lambda call-site is suspend, duplicate it   |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|crossinine    |variable     |ordinary      |call     |if lambda call-site is suspend, duplicate it   |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|crossinine    |variable     |suspend       |inline   |suspend function duplication                   |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|crossinine    |variable     |suspend       |call     |suspend function duplication                   |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|no lambda     |variable     |ordinary      |inline   |no suspend call allowed                        |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|ordinary|no lambda     |variable     |ordinary      |call     |no suspend call allowed                        |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |noinline      |block        |ordinary      |inline   |no state-machine with markers                  |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |noinline      |block        |suspend       |inline   |if called in a function, markers around invoke |
|        |              |             |              |         |if called in inner function, state-machine     |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |noinline      |variable     |ordinary      |inline   |no state-machine with markers                  |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |noinline      |variable     |ordinary      |call     |state-machine                                  |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |noinline      |variable     |suspend       |inline   |if called in a function, markers around invoke |
|        |              |             |              |         |if called in inner function, state-machine     |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |noinline      |variable     |suspend       |call     |state-machine with parameter's invoke in state |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |inline        |block        |ordinary      |inline   |suspend calls in block allowed,                |
|        |              |             |              |         |markers without state-machine                  |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |inline        |block        |suspend       |inline   |WARNING to remove `suspend` keyword            |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |inline        |variable     |ordinary      |inline   |no suspend call in argument allowed,           |
|        |              |             |              |         |markers without state-machine                  |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |inline        |variable     |ordinary      |call     |no suspend call in argument allowed,           |
|        |              |             |              |         |state-machine                                  |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |inline        |variable     |suspend       |inline   |WARNING should be ignored,                     |
|        |              |             |              |         |no state-machine with markers                  |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |inline        |variable     |suspend       |call     |WARNING should be ignored, state-machine       |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |crossinine    |block        |ordinary      |inline   |if lambda call-site is suspend, duplicate it   |
|        |              |             |              |         |function is duplicated                         |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |crossinine    |block        |suspend       |inline   |inline suspend markers around invoke           |
|        |              |             |              |         |if inline-site is inline, keep the template    |
|        |              |             |              |         |fake continuation for inliner to transform     |
|        |              |             |              |         |state-machine is built after inlining          |
|        |              |             |              |         |function is duplicated                         |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |crossinine    |variable     |ordinary      |inline   |if lambda call-site is suspend, duplicate it   |
|        |              |             |              |         |function is duplicated                         |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |crossinine    |variable     |ordinary      |call     |if lambda call-site is suspend, duplicate it   |
|        |              |             |              |         |function is duplicated                         |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |crossinine    |variable     |suspend       |inline   |suspend function duplication                   |
|        |              |             |              |         |function is duplicated                         |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |crossinine    |variable     |suspend       |call     |suspend function duplication                   |
|        |              |             |              |         |function is duplicated                         |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |no lambda     |variable     |ordinary      |inline   |no state-machine with markers                  |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
|suspend |no lambda     |variable     |ordinary      |call     |state-machine                                  |
+--------+--------------+-------------+--------------+---------+-----------------------------------------------+
```

## Callable Reference
Consider the following simple example of a callable reference to a suspend function:
```kotlin
import kotlin.coroutines.*

var c: Continuation<String>? = null

suspend fun callMe() = suspendCoroutine<String> { c = it }

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) { it.getOrThrow() })
}

suspend fun callSuspend(c: suspend () -> String) = c()

fun main() {
    builder {
        println(callSuspend(::callMe))
    }
    c?.resume("OK")
}
```
Instead of passing a lambda to the `callSuspend` function, we pass the callable reference. Inside the function, we call its
`invoke` method as if it were lambda. So, we need to generate an object with the method. However, unlike suspend lambda, the method
always calls only one function. Thus it can be tail-call. Since it is tail-call, we cannot use `BaseContinuationImpl` as a superclass.
Instead, we use `FunctionReferenceImpl`, which all callable references inherit. Additionally, since there is the `invoke` method, which
the object overrides, the object implements `Function{N+1}` interface, where `N` is the arity of the suspend function. Finally, it should
override the `SuspendFunction` marker interface as well, to support `is` and `as` suspend functional type checks.

Ideally, the object is a singleton, since it has no internal state. JVM_IR BE does so: it generates all callable references as singletons.
The old BE, however, does not generate callable references to suspend functions as singletons, which is a slip-up. Nevertheless, it is
unlikely to be addressed, since the new BE is going to replace the old one in the future and the slip-up is not critical enough to fix it
right away.

Finally, the old JVM BE generates suspending markers around the function call in the `invoke` method. It is a bug that is again fixed in
the JVM_IR BE and remains unfixed in the old BE.

### Inlining
Inlining of callable references to suspend functions is straightforward: from the inliner's point of view, a callable reference to suspend
function is as an inline lambda with a call. So, it should behave like a suspend lambda with only one call.

FIXME: Support suspend -> inline function conversions for callable references. Otherwise, even simple versions, like
`something?.let(MyClass::mySuspendMethod)` produce an error.

### Ordinary -> Suspend conversion
Consider the following example:
```kotlin
import kotlin.coroutines.*

fun callMe(): String = "OK"

var c: Continuation<Unit>? = null

suspend fun suspendMe() = suspendCoroutine<Unit> { c = it }

suspend fun callSuspend(c: suspend () -> String): String {
    suspendMe()
    return c()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) { it.getOrThrow() })
}

fun main() {
    builder {
        println(callSuspend(::callMe))
    }
    c?.resume(Unit)
}
```
Here, we pass a callable reference to an ordinary function to a function that expects suspend functional type. So, we cannot just pass
the callable reference object. Instead, we generate a so-called adapted function reference. It should not inherit `FunctionReference`
since adapted function references are not supported in reflection. So, instead of `FunctionReferenceImpl`, these objects inherit
`AdaptedFunctionReference`.

Unlike usual callable references, the one in the example should accept the continuation parameter, but it should ignore it since the
function is ordinary.

### Start
Unlike suspend lambdas, we cannot just call `create` when we start a coroutine (in a broad sense) from a callable reference. Since the
object does not have a `create` method and is not a continuation.

Hence, instead, we write the continuation by hand, and in the `invokeSuspend` function, we write a state-machine by hand as well. See
`createCoroutineFromSuspendFunction` for specifics.

FIXME: As explained in the tail-call suspend lambdas section, we can reuse this mechanism for tail-call suspend lambdas.

### Returning Inline Classes
The following example:
```kotlin
import kotlin.coroutines.*

inline class IC(val a: Any)

suspend fun callSuspend(c: suspend () -> Any) {
    println(c())
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) { it.getOrThrow() })
}

var c: Continuation<IC>? = null

suspend fun returnsIC() = suspendCoroutine<IC> { c = it }

fun main() {
    builder {
        callSuspend(::returnsIC)
    }
    c?.resume(IC("OK"))
}
```
used to throw KNPE inside `BaseContinuationImpl.resumeWith` before 1.4.20. The KNPE happens when we try to finish a coroutine (in a broad
sense) twice. Why wrapping a suspend function returning unboxed inline class leads to double finish?

The answer becomes apparent when we try to follow the execution of the example step by step.
1. `returnsIC` function returns `COROUTINE_SUSPENDED`.
2. The callable reference should return the boxed class since it overrides generic `FunctionN` and its `invoke` method returns the generic
type. Thus, it assumes that the function returns the unboxed type and boxes it.
3. The `BaseContinuationImpl.resumeWith` function does not receive `COROUTINE_SUSPENDED`, since it is boxed inside the inline class, and
runs shutdown procedure.
4. When we resume the execution, the suspend function returns the unboxed inline class.
5. The callable reference boxes it.
6. `BaseContinuationImpl.resumeWith` function runs shutdown procedure once again. The KNPE is thrown.

To fix the issue, callable references to suspend functions returning unboxed inline classes check for `COROUTINE_SUSPENDED` and only then
box the return value.

## Debug
Debugging coroutines is not as straightforward as debugging the usual code. For example, since a suspend call can suspend, even a simple
debugging event, like `step-over`, requires support from both codegen and debugger to be even possible. Another example is throwing an
exception from a coroutine, which might result in different stack traces depending on whether the coroutine has suspended and then resumed
or the suspension has never happened. If the coroutine has suspended and then resumed, the stack trace might not even have user code, only
the library code, making debugging a colossal PITA.

This section explains how the codegen provides the debugger information it needs.

### Step-over
Historically, the first debugging support in coroutines codegen was `step-over`. The call returns either a value or `COROUTINE_SUSPENDED`
marker. So, the state-machine checks the result and either continues the execution or returns the marker. Thus, the state-machine builder
generates two execution paths: for direct calls and suspend-resume. If the call returns a value, the `step-over` action runs as usual.
However, if we want to support the suspend-resume path, some tricks from both the codegen and the debugger are needed.

First of all, the codegen puts additional `LINENUMBER` instruction before returning the `COROUTINE_SUSPENDED` marker. This way, when a user
presses `step-over`, the debugger places a breakpoint at this `LINENUMBER` and removes it when the execution reaches either the next line
(in case of the direct path) or the breakpoint. The line number for this fictitious `LINENUMBER` is the line number of the caller's header.
We chose this number since we need line numbers to be different from function to function (there can be multiple suspended calls, waiting
for `step-over` to finish) and not interfering with user code.

When the coroutine resumes, the breakpoint is hit again, since the codegen generates another `LINENUMBER` at the start of the function.

To summarize, the codegen generates `N+1` fictitious `LINENUMBER` instruction in the suspend-resume path, so the debugger can place a
breakpoint on the line number and emulate `step-over` with a single breakpoint, which is hit twice.

### Debug Metadata
The continuation-passing-style explained that when we resume a coroutine, its caller becomes `BaseContinuationImpl.resumeWith`. However,
the user wants to see a coroutine that called the suspended one. Fortunately, the information about the caller can be obtained through the
completion chain. The stacktrace, constructed using the chain, is called Async Stack Trace.

However, first, the codegen should provide the information. To do this, every continuation class has
`@kotlin.coroutines.jvm.internal.DebugMetadata` annotation, which contains the following fields:
1. The source file, class name, and method name, along with line numbers, are used to generate async stack trace elements.
2. An array of line numbers is a map from the `label` value to the suspension point line number.
3. Mapping from spilled variables to locals, which the debugger uses to show the values of the variables.

Both the debugger and `kotlinx.coroutines` use debug metadata to generate async stack traces.

Continuation's `toString` method uses the debug metadata to show the location where the coroutine is suspended.

Note that tail-call optimization removes continuations. So, there are gaps in the async stack trace. Additionally, only line numbers of
suspension points are stored; thus, line numbers are not always accurate.

Since 2.2.20 - we store linenumbers of next statements after suspension points in the metadata annotation.
The debugger is expected to use this information to set a breakpoint there when step-over is pressed.
This way, step-over works even if the suspension point suspends. 

### Probes
`kotlin.coroutines.jvm.internal` package contains probe functions replaced by the debugger to show current coroutines (in a broad sense)
and their statuses: suspended or running.
1. `probeCoroutineCreate` is invoked in `createCoroutine` function.
2. `probeCoroutineResumed` is invoked in `BaseContinuationImpl.resumeWith` function.
3. `probeCoroutineSuspended` call is generated by the codegen if `suspendCoroutineUninterceptedOrReturn` returns `COROUTINE_SUSPENDED`.

### Spilling

The compiler cleans up dead variables by calling `kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable`. The debugger is
expected to replace the variable, just like probes.

See "Spilled Variables Cleanup" section.

### Generated Code Markers
`-Xenhanced-coroutines-debugging` forces the compiler to generate additional linenumbers before the compiler generated code in suspend
functions and lambdas in order to distinguish them from user code. Additionally, LVT entries are added.

The additional linenumbers mark
1. Continuation check at the beginning of a suspend function.
2. Unspilling of arguments of a suspend lambda.
3. State-machine header (TABLESWITCH of the state-machine).
4. Check of $result variable at the beginning of the state-machine and after each suspend call.
5. Check for COROUTINE_SUSPENDED marker after each suspend call.
6. Default label, which throws IllegalStateException - which is unreachable in normal execution.

The additional LVT entries are
1. $ecd$checkContinuation$<linenumber>
2. $ecd$lambdaArgumentsUnspilling$<linenumber>
3. $ecd$tableswitch$<linenumber>
4. $ecd$checkResult$<linenumber>
5. $ecd$checkCOROUTINE_SUSPENDED$<linenumber>
6. $ecd$unreachable$<linenumber>

These linenumbers are mapped to `GeneratedCodeMarkers.kt` file in stdlib via SMAP by inliner, and point to one-line marker inline function.
This way, even if unsupported version of debugger is used, it will assume, that this is just normal inlining taking place. 
