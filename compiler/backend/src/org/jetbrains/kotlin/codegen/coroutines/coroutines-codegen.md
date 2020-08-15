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
which, upon running, will print the exception. Note, that it is printed inside the `builder` function (because of 
`println(result.exceptionOrNull())`). There are a couple of things happening here: one is inside the generated
state machine, and the other is inside `BaseContinuationImpl`'s `resumeWith`.

First, we change the generated state machine. As explained before, the type of `$result` variable is `Int | COROUTINE_SUSPENDED | 
Result$Failue(Throwable)`, but when we resume, by convention, its type cannot be `COROUTINE_SUSPENDED`. Still, the type is `Int | 
Result$Failure(Throwable)`, which we cannot just pass to `plus`, at least, without a check and `CHECKCAST`. Otherwise, we will get CCE at 
runtime. Thus, we check the `$result` variable and throw the exception if the variable holds it.
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
there is no explicit try-catch block. Thankfully, we can propagate the exception the same way as the execution upon coroutine's completion, 
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

#### 1.2: Data and Exception

Since 1.3 introduced inline classes and `Result` as one of them, experimental coroutines use a different approach to passing value and 
exception to `doResume`, which was the name of `invokeSuspend` in experimental coroutines.
Instead of one parameter with type `returnType | COROUTINE_SUSPENDED | Result$Failure(Throwable)`, experimental
coroutines' suspend lambda's `doResume` accepts two parameters: `data` and `exception`. `data` has type `returnType | COROUTINE_SUSPENDED` 
and `exception` has type `Throwable`. `resume` and `resumeWithException` used to be methods of `Continuation` interface and in 1.3 they 
were replaced by `resumeWith`. `resume` and `resumeWithException` are now extension functions on `Continuation`, which call `resumeWith`.

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
here, we should save `a1` before `suspendMe`, and we should restore it after the resumption. Similarly, we should save both `a1` and `a2` 
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
Furthermore, it cleans the field for spilled variables of reference types up to avoid memory leaks by pushing `null` to it so that GC can 
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
After line (1) `a` is dead, but `b` is still alive. So, we spill only `b`. There is no variable alive after line (2), but the continuation 
object still holds a reference to `b` in the `L$0` field. So, to clean it up and avoid memory leaks, we push `null` to it.

Generally, the compiler generates spilling and unspilling code so that it uses only the first fields. If there are M fields for references, 
but we spill only N (where N ≤ M, of course) objects at the suspension point, everything else should be `null`. However, we do not need to 
nullify all of them every suspension point. Instead, the compiler checks which of the fields hold references and clears only them.

Additionally, the compiler shrinks and splits LVT records for local variables, so a debugger will not show dead variables as uninitialized.

FIXME: Currently, dead variables do not present in LVT. So, if a programmer defines a variable but does not use it, the compiler removes the 
LVT record for the variable. We can ease this restriction and assume the variable to be alive until the following suspension point.

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
continuation parameter to it. Since its purpose is to give access to the continuation argument, which is invisible in suspend functions 
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
versions of the function: one is to start a suspend lambda without parameters and the other one - to start a coroutine with either one 
parameter or a receiver. It is defined as follows:
```kotlin
public fun <T> (suspend () -> T).startCoroutine(completion: Continuation<T>) {
    createCoroutineUnintercepted(completion).intercepted().resume(Unit)
}
```
So, it
1. creates a coroutine
2. intercepts it
3. starts it

Once again, `createCoroutineUnintercepted` has two versions - one without parameters and the other one with exactly one parameter. All it
does is calling suspending lambda's `create` function. After the interception, we resume the coroutine with a dummy value. As I explained 
in the resume with the value section, the state-machine ignores the value in its first state. Thus, it is the perfect way to start a 
coroutine without calling `invokeSuspend`. However, the way we start callable references is different. Since they are tail-call, in other 
words, do not have a
continuation inside an object, we wrap them in a hand-written one.

#### create

`create` is generated by the compiler and it
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
    val resutl = main$lambda$1(this.$i, completion)
    result.I$0 = value as Int
}
```
note that the constructor, in addition to captured parameters, accepts a completion object.

In Old JVM BE, `create` is generated for every suspend lambda even when we do not need the function. I.e., even for suspending lambdas with 
more than one parameter. There are only two versions of `createCoroutineUnintercepted`, and there are no other places where we call 
`create` (apart from compiler-generated `invoke`s). Thus, in JVM_IR BE, we fixed the slip-up, and it generates the `create` function only 
for functions with zero on one parameter.

##### Lambda Parameters

We need to put the arguments of the suspend lambda into fields since there can be only one argument of `invokeSuspend` - `$result`.
The compiler moves the lambda body into `invokeSuspend`. Thus, `invokeSuspend` does all the computation. We reuse fields for spilled 
variables for parameters as well. For example, if we have a lambda with type `suspend Int.(Long, Any) -> Unit`, then `I$0` hold value of 
extension receiver,' `J$0` - the first argument, `L$1` - the second one.

This way, we can reuse spilled variables cleanup logic for parameters. If we used separate fields for parameters, we would need to manually 
push `null` to them as we do for spilled variable fields if we do not need them anymore.

#### invoke

`invoke` is basically `startCoroutine` without an interception. In `invoke`, we call `create` and resume a new instance with dummy value by 
calling `invokeSuspend`. We cannot just call `invokeSuspend` without calling the constructor first is that it would not create a 
continuation needed for the completion chain, as explained in the continuation-passing style section. Also, recursive suspend lambda calls 
would reset `label`'s value.

FIXME: We do not need to create an additional copy of the lambda if we can verify that we do not pass them as completion to themselves. 
However, this includes not only recursive lambdas. We can pass the lambda to a tail-call suspending function and call it there. In this 
case, the continuation object is the same, and we have the same problems as if there was a recursion.

Of course, in JVM_IR, we do not have a `create` function in case when the lambda has more than one parameter, `invoke` creates a new 
instance of the lambda with copies of all captured variables and then puts the parameters of the lambda to fields.