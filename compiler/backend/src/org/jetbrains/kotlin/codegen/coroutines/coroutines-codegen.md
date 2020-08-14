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
lisps. For example, in Scheme, if a function returns a value in a continuation-passing style, it passes the value to the continuation 
parameter. So, a function accepts the continuation parameter, and the caller passes the continuation by calling `call/cc` intrinsic. The
 same happens in Kotlin with passing return value to caller's continuation's `resumeWith`. However, unlike Scheme, Kotlin does not use 
 something like `call/cc`. Every coroutine already has a continuation. The caller passes it to the callee as an argument. Since the 
 coroutine passes the return value to `resumeWith`, its parameter has the same type as the return type of the coroutine. Technically, the 
 type is `Result<T>`, but it is just a union `T | Throwable`; in this case, `T` is `Unit`. The next section uses return types other than 
 `Unit` to illustrate how to resume a coroutine with a value. The other part, `Throwable`, is for resuming a coroutine with an exception 
 and is explained in the relevant section.

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
`suspendMe`'s continuation has type `Continuation<Int>`, i.e., the return type of the function is used as a type argument of `Continuation` 
interface, as I mentioned in the previous section (about continuation-passing style).

The state-machine section touched upon the `$result` variable inside the `invokeSuspend` function. The listing shows the `invokeSuspend` 
function of `a`, but, unlike the previous example, with its signature:
```kotlin
fun invokeSuspend($result: Any?): Any? {
    when(this.label) {
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
Following the resume process, `resume` calls continuation's `resumeWith`, which calls `invokeSuspend` with
value `42`. Then, this will be `$result` and work the same as if `suspendMe` returned `42`. In other words, `suspendCoroutine` with an 
unconditional resume will not suspend the coroutine and is semantically the same as returning the value.

It is important to note that passing `COROUTINE_SUSPENDED` to continuation's `resumeWith` leads to undefined behavior.