# Sequence fusion lowering

`SequenceFusionLowering` ([KT-27000](https://youtrack.jetbrains.com/issue/KT-27000)) is a JVM file lowering that rewrites
a chain of `kotlin.sequences.Sequence` operations

```
producer -> transformer* -> consumer
```

into one `while` loop. The fused code creates no intermediate `Sequence` or `Iterator` objects, and it calls the lambdas of
the operations directly in the loop. The observable behavior must stay the same: element order, laziness, how many times
each argument is evaluated, and exceptions and their messages. If the lowering can't prove that, it leaves the original
code in place.

## Contents

1. [Where the phase runs](#where-the-phase-runs)
2. [Pipeline](#pipeline)
3. [Core model](#core-model)
4. [Supported operations](#supported-operations)
5. [Safety rules: when fusion is skipped](#safety-rules-when-fusion-is-skipped)
6. [Examples per producer](#examples-per-producer)
7. [Benchmarks](#benchmarks)
8. [Testing](#testing)
9. [Adding a new operation](#adding-a-new-operation)

## Where the phase runs

The phase is registered in `JvmLoweringPhases.kt` (`jvmFilePhases`), right before `VarargLowering`. It depends on IR
shapes that later lowerings remove:

| It needs                                                                     | Removed by                                            |
|------------------------------------------------------------------------------|-------------------------------------------------------|
| `IrVararg` arguments of `sequenceOf(...)`                                    | `VarargLowering`                                      |
| `for` loops with `FOR_LOOP` origin                                           | `ForLoopsLowering`                                    |
| Lambdas and references as `IrRichFunctionReference` with an `invokeFunction` | `FunctionReferenceLowering`, `SuspendLambdaLowering`  |

The phase is always enabled. To compile without it, pass
`-Xdisable-phases=SequenceFusionLowering`.

## Pipeline

`SequenceFusionLowering.lower` runs two passes over each `IrFile`:

1. **`ReusedSequenceMarker`** (`ReusedSequenceMarker.kt`) is a read-only visitor. For every variable whose initializer
   has a sequence type, it records:
    - `readCounter`: the number of places where the variable is read;
    - `isReadInRepeatedContext`: whether some read is nested deeper than the declaration inside a loop, a function, or
      a class. Such a read can run many times.

   The gatherer uses these to decide whether the chain stored in the variable may be fused entirely,
   or if the sequence in the variable should be treated as a sequence with no additional information.

2. **`SequenceFusionTransformer`** (`SequenceFusionLowering.kt`) works bottom-up: inner
   loops and consumers are fused before outer ones. A fusion candidate is a `for` loop over a sequence or a call to a
   supported consumer; its receiver is the sequence being iterated or consumed. For each candidate, `fuse` does the
   following:
    1. `SequenceDataGatherer` visits the receiver and attaches `SequenceData` to it (`sequenceDataOfExpression`).
       If there is none, the code is left unchanged.
    2. `createConsumerStrategy` picks a `ConsumerStrategy`. `createSequenceReplacement()` returns a
       `SequenceReplacement`, or `null` if the consumer can't be fused.
    3. `addTransformers` walks `sequenceData.transformers` **in reverse** (from the consumer back to the source). Each
       `TransformerStrategy` wraps the replacement built so far.
    4. `SequenceSource.createProducerStrategy` picks a `ProducerStrategy`. `fuseConsumer` emits the final block or
       returns `null` if the producer refuses.

   Any `null` keeps the original expression.

## Core model

### `SequenceData` (`SequenceData.kt`)

`SequenceData` describes what a sequence expression is built from:

| Field                    | Meaning                                                                                                                                         |
|--------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| `sequenceSource`         | The producer (`SequenceSource`)                                                                                                                 |
| `transformers`           | The transformer calls applied to it, in source order                                                                                            |
| `isMovable`              | Whether the chain may be re-evaluated at the place of use. It is false when a `var`, or an argument that isn't safe to re-evaluate, is involved |

There are three other fields in `SequenceData`, and they are all used to prevent exponential growth when copying
variables:

| Field                    | Meaning                                                                       |
|--------------------------|-------------------------------------------------------------------------------|
| `isVariableCopy`         | This data was copied from a variable to a read of it                          |
| `isCopiedToReads`        | Reading a variable with this data puts a copy of the sequence's code there    |
| `containsVariableCopies` | Whether the chain already contains fused copies of other sequence variables   |

Reading a variable whose data `isCopiedToReads` doesn't just refer to the variable: the read gets a copy of the
sequence's code, and its data is marked `isVariableCopy`. For example, in `val seq = sequenceOf(1); val seq2 = seq`,
the read of `seq` is a variable copy, so `seq2`'s data has `isVariableCopy` set.

### `SequenceSource`

| Variant            | Created from                                                                                                                                                   | Producer strategy          |
|--------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `SequenceOf`       | `sequenceOf(a, b, ...)` with no spread  arguments                                                                                                              | `SequenceOfStrategy`       |
| `Empty`            | `emptySequence()`, `sequenceOf()`                                                                                                                              | `EmptySequenceStrategy`    |
| `AsSequence`       | `Iterable.asSequence()`                                                                                                                                        | `UnknownVariableStrategy`  |
| `Sequence`         | `sequence { ... }` with a lambda literal                                                                                                                       | `SequenceBuilderStrategy`  |
| `GenerateSequence` | `generateSequence`                                                                                                                                             | `GenerateSequenceStrategy` |
| `Variable`         | a sequence variable or parameter whose chain isn't known: a parameter, a `var`, a `val` that can't be copied, or a `val` holding the result of an unknown call | `UnknownVariableStrategy`  |

Only the calls listed above are recognized as producers. Any other call that returns a sequence, such as
`Array.asSequence()` or a user function, is fused only when its result is stored in a `val` first, which makes it a
`Variable` source. Used directly in a chain, as in `produce().map { ... }.toList()`, it isn't fused.

### `SequenceReplacement` (`SequenceFusionLowering.kt`)

This is the interface between the strategies. A consumer creates it, every transformer receives one and returns
a new one, and the producer receives it:

| Field                 | Meaning                                                                                    |
|-----------------------|--------------------------------------------------------------------------------------------|
| `initialDeclarations` | Statements evaluated once, in order, before the iteration                                  |
| `skipIteration`       | Evaluated once, after `initialDeclarations`; if `true`, the producer skips the iteration   |
| `mainBodyBuilder`     | Builds the body for one element; it evaluates to `true` to continue and `false` to stop    |
| `finalExpression`     | Evaluated after the iteration; the value of the whole fused expression                     |
| `leadingDropCounts`   | The arguments of the `drop`s directly downstream, with only `take`s and `drop`s in between |

`skipIteration` is `true` when the sequence is known to be empty before it starts: `take(0)`, or `take(n).drop(m)`
with `m >= n`. The producer then skips the whole iteration, including work such as calling `iterator()` or a seed
function. `leadingDropCounts` exists for the second case: `take` uses it to detect that the `drop`s after it remove everything it takes.

## Supported operations

FQ-name constants are in `SequenceDataGathererer.kt` (producers and transformers) and `consumers/ConsumerStrategy.kt`
(consumers).

| Kind        | Operations                                                                                               | Strategy                    |
|-------------|----------------------------------------------------------------------------------------------------------|-----------------------------|
| Producer    | `sequenceOf`                                                                                             | `SequenceOfStrategy`        |
| Producer    | `emptySequence`                                                                                          | `EmptySequenceStrategy`     |
| Producer    | `Iterable.asSequence`, sequence variables and parameters                                                 | `UnknownVariableStrategy`   |
| Producer    | `sequence { yield / yieldAll }`                                                                          | `SequenceBuilderStrategy`   |
| Producer    | `generateSequence`                                                                                       | `GenerateSequenceStrategy`  |
| Transformer | `map`, `mapIndexed`, `mapNotNull`, `mapIndexedNotNull`                                                   | `MapStrategy`               |
| Transformer | `filter`, `filterNot`, `filterNotNull`                                                                   | `FilterStrategy`            |
| Transformer | `take`                                                                                                   | `TakeStrategy`              |
| Transformer | `drop`                                                                                                   | `DropStrategy`              |
| Transformer | `takeWhile`, `dropWhile`                                                                                 | `TakeWhileStrategy`         |
| Transformer | `withIndex`                                                                                              | `WithIndexStrategy`         |
| Consumer    | `for` loop                                                                                               | `ForLoopStrategy`           |
| Consumer    | `forEach`, `forEachIndexed`                                                                              | `ForEachStrategy`           |
| Consumer    | `first`, `firstOrNull`, `last`, `lastOrNull`, `find`, `findLast`                                         | `FirstLastStrategy`         |
| Consumer    | `firstNotNullOf`, `firstNotNullOfOrNull`                                                                 | `FirstNotNullOfStrategy`    |
| Consumer    | `indexOf`, `indexOfFirst`, `indexOfLast`                                                                 | `IndexOfStrategy`           |
| Consumer    | `filterTo`, `filterNotTo`, `filterNotNullTo`                                                             | `FilterToStrategy`          |
| Consumer    | `toList`, `toMutableList`, `toSet`, `toMutableSet`, `toCollection`                                       | `ToCollectionStrategy`      |

`ToCollectionStrategy` reproduces the stdlib's result types. Read-only `toList()`/`toSet()` of zero or one elements
return `emptyList()`/`listOf(e)` or `emptySet()`/`setOf(e)`; otherwise they return an `ArrayList` or `LinkedHashSet`.

### `ProducerStrategy.fuseConsumer` (`producers/ProducerStrategy.kt`)

Producers take the final `SequenceReplacement` and emit roughly this shape:

```
initialDeclarations
if (!skipIteration) {
    <work before the first element, e.g. iterator() or the seed function>
    loop {
        val element = <next element>
        val shouldContinue = mainBodyBuilder(element)
        if (!shouldContinue) break
    }
}
finalExpression
```
There are two ways the iteration ends early, with separate roles:
- `skipIteration` is decided **before the iteration starts**, from the state of the operations alone. It is the only
  way to avoid requesting the first element.
- The `Boolean` returned by `mainBodyBuilder` is decided **after each element** and is the only way to stop once the
  iteration has started. Every transformer that calls the downstream body must return `false` as soon as the
  downstream body does.
## Safety rules: when fusion is skipped

The fused code copies and moves expressions, so every argument is checked first.

### Re-evaluation

These functions are in `SequenceDataGathererer.kt` and `SequenceFusionLowering.kt`:
- `isSafeToReevaluate()` decides whether `sequenceOf` elements, `asSequence` receivers, the `generateSequence` seed, and
  `take`/`drop` arguments may move. It allows constants, `val` reads, final field reads, primitive/String intrinsics,
  and string templates of primitives and Strings. It rejects other calls, constructors, assignments, `var` reads, and
  non-final fields.
- `isFunctionExpressionSafeToReevaluate()` covers function arguments. It allows lambdas and references whose bound
  values are stable reads (`isStableRead`), `val` reads, and implicit casts of these.
- Transformers call `getPredicateArgument(..., isEvaluatedOnce = true)`, which also accepts other function values. These are stored in a
  temporary, and the chain is marked `isMovable = false`. Consumers call
  `getPredicateArgument` without it, so their function arguments must be safe to re-evaluate; otherwise the consumer
  isn't fused.

### Lambdas that can't be inlined

`containsUnlowerableCalls` rejects lambdas that make suspend calls or `SequenceScope` calls. Only the `sequence { }`
body itself may call `yield`/`yieldAll`. 

### `sequenceOf`

A spread argument (`sequenceOf(*array)`) is not fused.

### Sequence variables (`SequenceDataGatherer.visitVariable` / `visitGetValue`)

- A `var` of sequence type stays a `Variable` source with `isMovable = false`, because it could be reassigned before
  use.
- A `val` whose data is not movable becomes a plain `Variable` source and is iterated through `iterator()`.
- A `val` initialized with `generateSequence(nextFunction)` is constrained to one iteration. If it is reused (read more
  than once, or in a repeated context), it is not copied, so the stdlib still throws on the second iteration.

- **Exponential growth.** Copying a chain to every read can multiply code size, for example
  `val b = sequence { yieldAll(a); yieldAll(a) }; val c = sequence { yieldAll(b); yieldAll(b) }`. A `val` read more than
  once whose data already `containsVariableCopies` is therefore not copied. Fused copies are marked
  `isFusedVariableCopy`, and `containsFusedVariableCopy()` finds them, including `yieldAll` arguments inside builders.

### `sequence { }` builder (`producers/SequenceBuilderStrategy.kt`)

The builder body is inlined at the use site. Every `yield(x)` becomes `if (!sequenceYieldConsumer(x)) return`, where
`sequenceYieldConsumer` is a local function holding the consumer body. A local function is used because inlining the
consumer at every `yield` would break consumers that expect their body to appear once, and it could grow the code
quadratically. Fusion is skipped when:
- the use site is inside an inline function, because code like `it is T` for a reified `T` isn't valid in the local
  function
- the body contains suspend calls other than `yield`/`yieldAll`
- a `yield` is inside a `try`, or inside a call that could observe the consumer's exceptions. Only the inline
  functions in `functionsTransparentForYields` are allowed
- the consumer contains jumps or returns that target code outside it
- `this` of the `SequenceScope` is used other than as the receiver of `yield`/`yieldAll`
- `yieldAll` gets an argument type that isn't supported

`yieldAll` is handled by `YieldReplacer`:
- An `Iterator` argument becomes a `hasNext`/`next` loop.
- An `Iterable` argument does the same, but first skips `iterator()` for an empty `Collection`. Literal
  `listOf`/`mutableListOf`/`arrayListOf` arguments are fused like `sequenceOf`.
- A `Sequence` argument is fused recursively (`fuseYieldAll`), and a `yieldAllStopped` flag carries early termination
  out to the outer builder.

## Examples per producer

These are simplified; the real IR also contains overflow checks and temporaries.

**`sequenceOf`** produces a `when` over an index, which compiles to `TABLESWITCH`. The elements are evaluated eagerly:

```kotlin
sequenceOf(7, 5).map { it - 1 }.forEach { println(it) }
// ->
var i = 0
val e0 = 7; val e1 = 5
while (i < 2) {
    val currentElement = when (i) { 0 -> e0; 1 -> e1; else -> throw NoWhenBranchMatchedException() }
    val mapResult = currentElement - 1
    println(mapResult)
    i++
}
```

**`asSequence` / unknown sequence** iterates through the original `iterator()`:

```kotlin
fun f(seq: Sequence<Int>) = seq.filter { it > 0 }.firstOrNull()
// ->
val iterator = seq.iterator()
var result: Int? = null
while (iterator.hasNext()) {
    val element = iterator.next()
    if (element > 0) { result = element; break }
}
result
```

**`generateSequence(seed, next)`** keeps a nullable state variable. The seed is evaluated eagerly; the `seedFunction` and
`nextFunction` overloads evaluate their first value only when the iteration starts:

```kotlin
generateSequence(1) { it * 2 }.take(3).toList()
// ->
var state: Int? = 1
var count = 0
val n = 3
if (n < 0) throw IllegalArgumentException("Requested element count " + n + " is less than zero.")
val destination = ArrayList<Int>()
if (!(n <= 0)) {                  // skipIteration of take
    while (state != null) {
        val element = state!!
        count++
        destination.add(element)
        if (!(count < n)) break
        state = element * 2
    }
}
destination  // or emptyList()/listOf(e) for zero or one elements
```

**`sequence { }`**:

```kotlin
sequence { yield(1); yieldAll(listOf(2, 3)) }.forEach { println(it) }
// ->
fun sequenceYieldConsumer(element: Int): Boolean { println(element); return true }
run {
    if (!sequenceYieldConsumer(1)) return@run
    /* listOf(2, 3) fused like sequenceOf(2, 3) */
    var i = 0
    val e0 = 2; val e1 = 3
    while (i < 2) {
        val currentElement = when (i) { 0 -> e0; 1 -> e1; else -> throw NoWhenBranchMatchedException() }
        if (!sequenceYieldConsumer(currentElement)) return@run
    }
}
```

## Benchmarks

These JMH results compare code compiled by the same compiler with this phase (**fused**) and without it
(**baseline**), so the phase is the only difference. All benchmarks run on the JVM in `AverageTime` mode; lower is
better. *Speedup* is baseline ÷ fused, so values below 1× are regressions. It is marked *(within error)* when the two
± ranges overlap.

- **Compiler:** `dist/kotlinc` built from this branch at `7091ce0970f3`.
- **Variants:** every benchmark is compiled twice from the same source, into the package `fused` (default options)
  and into `baseline` (`-Xdisable-phases=SequenceFusionLowering`). `check-fusion.sh` confirms in the bytecode that no
  `fused` method calls `kotlin.sequences` functions and that every `baseline` method still does. The only exception is
  the `Sequence` parameter case, which by design keeps one `iterator()` call.
- **Machine:** Intel Core Ultra 9 285H (6 performance, 8 efficiency, and 2 low-power efficiency cores), JDK 17.0.20.1
  (OpenJDK 64-Bit Server VM, default VM options). All benchmark JVMs are pinned to the performance cores with
  `taskset`, which `run.sh` does, and the system power profile was set to `performance` for the run. Without these
  controls, the benchmark thread moved between cores running at about 2.3 and 5.1 GHz, so scores jumped between two
  levels about 2× apart, even within one fork.
- **JMH:** JMH 1.36 in `AverageTime` mode, with both variants in the same session. Each microbenchmark score comes
  from 3 forks × 5 measurement iterations of 1 s, after 5 warmup iterations of 1 s (15 measurements). The order
  pipeline uses 5 forks × 10 measurement iterations (50 measurements). The median benchmark varies by 4.5% between
  its fastest and slowest iteration, and the least stable one (the `generateSequence` baseline) by 32%.
- **Allocation:** measured with `-prof gc` (`gc.alloc.rate.norm`, bytes per operation). The *Allocation* column is
  the change from baseline to fused. −100% means the fused variant allocates nothing, and "N× more" is used when it
  allocates more than twice as much.
- **Inputs:** they come from `@State` fields set in `@Setup`, so the JIT can't constant-fold the pipelines, and one
  operation is one pipeline run. Unless stated otherwise, the input is a `List<Int>` of 1000 elements.
- **Blackholes:** JMH auto-selected *compiler blackholes*, so `Blackhole.consume` costs almost nothing. That is why the
  fully fused tiny pipelines measure below 1 ns: they are a handful of integer additions per operation.

**Chain length** (`asSequence()`, then *n* × `map { it + k }`, then `forEach`)

| Maps | Baseline (ns/op)  | Fused (ns/op) | Speedup | Baseline B/op | Fused B/op | Allocation |
|------|-------------------|---------------|---------|---------------|------------|------------|
| 1    | 292 ± 2.27        | 226 ± 3.35    | 1.29×   | 48            | 0          | −100%      |
| 2    | 2 696 ± 153       | 240 ± 7.44    | 11×     | 14 064        | 0          | −100%      |
| 4    | 9 858 ± 119       | 240 ± 6.16    | 41×     | 56 320        | 0          | −100%      |
| 8    | 20 178 ± 77.7     | 244 ± 3.62    | 83×     | 113 920       | 0          | −100%      |
| 16   | 48 182 ± 1 050    | 234 ± 6.31    | 206×    | 236 932       | 0          | −100%      |

**Input size** (`asSequence().filter { it % 2 == 0 }.map { it * 3 }.toList()`)

| Size      | Baseline (ns/op)      | Fused (ns/op)       | Speedup              | Baseline B/op | Fused B/op | Allocation |
|-----------|-----------------------|---------------------|----------------------|---------------|------------|------------|
| 1         | 12.1 ± 0.75           | 8.39 ± 0.33         | 1.44×                | 152           | 80         | −47%       |
| 10        | 30.9 ± 2.29           | 24.2 ± 0.65         | 1.28×                | 208           | 80         | −62%       |
| 100       | 377 ± 8.18            | 319 ± 2.69          | 1.18×                | 1 520         | 1 392      | −8%        |
| 10 000    | 33 746 ± 718          | 30 986 ± 625        | 1.09×                | 155 040       | 154 888    | ±0%        |
| 1 000 000 | 4 152 410 ± 260 983   | 3 143 545 ± 53 314  | 1.32×                | 14 482 914    | 14 482 777 | ±0%        |

**Transformers** (one transformer, then `forEach`)

| Benchmark                                  | Baseline (ns/op) | Fused (ns/op) | Speedup      | Baseline B/op | Fused B/op | Allocation |
|--------------------------------------------|------------------|---------------|--------------|---------------|------------|------------|
| `map { it * 3 }`                           | 312 ± 1.12       | 259 ± 4.67    | 1.20×        | 48            | 0          | −100%      |
| `mapIndexed { i, v -> i + v }`             | 1 673 ± 15.4     | 253 ± 2.35    | 6.62×        | 14 000        | 0          | −100%      |
| `mapNotNull { it?.plus(1) }` (⅓ `null`s)   | 2 017 ± 13.3     | 1 075 ± 24.7  | 1.88×        | 9 408         | 9 312      | −1%        |
| `filter { it % 3 == 0 }`                   | 1 060 ± 3.47     | 780 ± 14.7    | 1.36×        | 32            | 0          | −100%      |
| `filterNotNull()` (⅓ `null`s)              | 775 ± 7.04       | 604 ± 12.6    | 1.28×        | 32            | 0          | −100%      |
| `take(500)`                                | 203 ± 1.33       | 110 ± 2.33    | 1.85×        | 48            | 0          | −100%      |
| `drop(500)`                                | 425 ± 7.92       | 1 096 ± 10.8  | **0.39×**    | 48            | 0          | −100%      |
| `takeWhile { it < 500 }`                   | 221 ± 2.90       | 118 ± 2.31    | 1.88×        | 32            | 0          | −100%      |
| `dropWhile { it < 500 }`                   | 1 091 ± 91.0     | 429 ± 4.09    | 2.54×        | 104           | 0          | −100%      |
| `withIndex()`                              | 514 ± 7.49       | 1 534 ± 17.3  | **0.34×**    | 48            | 13 952     | 291× more  |

**Consumers** (`asSequence().map { it + 1 }`, then the consumer)

| Benchmark                                 | Baseline (ns/op) | Fused (ns/op) | Speedup              | Baseline B/op | Fused B/op | Allocation |
|-------------------------------------------|------------------|---------------|----------------------|---------------|------------|------------|
| `for` loop                                | 289 ± 1.62       | 235 ± 2.24    | 1.23×                | 48            | 0          | −100%      |
| `forEachIndexed`                          | 1 750 ± 15.9     | 283 ± 4.17    | 6.19×                | 14 016        | 0          | −100%      |
| `toList()`                                | 3 556 ± 40.9     | 4 794 ± 84.2  | **0.74×**            | 29 064        | 28 992     | ±0%        |
| `toMutableList()`                         | 3 739 ± 29.2     | 4 738 ± 54.1  | **0.79×**            | 29 040        | 28 992     | ±0%        |
| `toSet()` (`map { it % 100 }`)            | 6 376 ± 101      | 6 183 ± 62.5  | 1.03×                | 6 184         | 6 136      | −1%        |
| `filterTo(ArrayList()) { it % 2 == 0 }`   | 3 336 ± 32.3     | 2 972 ± 35.1  | 1.12×                | 20 776        | 13 752     | −34%       |
| `first { it == 1000 }` (last element)     | 1 718 ± 18.1     | 1 865 ± 4.61  | **0.92×**            | 14 016        | 16         | −100%      |
| `firstOrNull { it < 0 }` (no match)       | 1 741 ± 55.9     | 279 ± 3.82    | 6.25×                | 14 016        | 0          | −100%      |
| `last()`                                  | 1 747 ± 17.6     | 1 543 ± 18.9  | 1.13×                | 14 016        | 13 968     | ±0%        |
| `findLast { it % 7 == 0 }`                | 1 959 ± 12.1     | 1 170 ± 20.0  | 1.67×                | 14 016        | 1 984      | −86%       |
| `indexOfFirst { it == 900 }`              | 1 563 ± 44.8     | 277 ± 17.4    | 5.65×                | 12 416        | 0          | −100%      |
| `indexOfLast { it % 7 == 0 }`             | 2 009 ± 52.3     | 1 138 ± 15.6  | 1.77×                | 14 016        | 0          | −100%      |
| `firstNotNullOfOrNull` (match at 900)     | 869 ± 15.3       | 840 ± 17.3    | 1.03× (within error) | 96            | 48         | −50%       |

**Other producers**

| Benchmark                                                                  | Baseline (ns/op) | Fused (ns/op) | Speedup | Baseline B/op | Fused B/op | Allocation |
|----------------------------------------------------------------------------|------------------|---------------|---------|---------------|------------|------------|
| `sequenceOf` of 5 non-constant `Int`s, 10 `map`s, `for`                    | 190 ± 2.97       | 0.32 ± 0.00   | (600×)  | 512           | 0          | −100%      |
| `sequenceOf` of 5 non-constant `Int`s, 1 `map`, `for`                      | 9.36 ± 0.57      | 0.32 ± 0.00   | (29×)   | 64            | 0          | −100%      |
| `generateSequence(0) { ... }` of 1001 elements, `filter`, `map`, `forEach` | 3 095 ± 383      | 1 691 ± 50.9  | 1.83×   | 14 104        | 13 968     | −1%        |
| `sequence { yield(a); yieldAll(list of 99) }`, 4 `map`s, `forEach`         | 1 154 ± 7.66     | 42.8 ± 0.92   | 27×     | 1 552         | 0          | −100%      |
| `Sequence<Int>` parameter (100 elements), 3 `map`s, `toList`               | 944 ± 11.2       | 345 ± 4.67    | 2.74×   | 3 616         | 1 672      | −54%       |

**`take` chains**

| Benchmark                                                             | Baseline (ns/op) | Fused (ns/op) | Speedup              | Baseline B/op | Fused B/op | Allocation |
|-----------------------------------------------------------------------|------------------|---------------|----------------------|---------------|------------|------------|
| `generateSequence("a") { it + "a" }.take(10)`, `for`                  | 77.0 ± 0.70      | 71.2 ± 5.89   | 1.08× (within error) | 512           | 448        | −12%       |
| `generateSequence(1) { it + 1 }.take(10)`, `for`                      | 38.2 ± 0.27      | 2.29 ± 0.02   | 17×                  | 64            | 0          | −100%      |
| list of 100, `take(90)`, `forEach`                                    | 57.6 ± 0.64      | 23.2 ± 0.48   | 2.48×                | 48            | 0          | −100%      |
| list of 100, `take(99)...take(90)` (10 `take`s), `forEach`            | 223 ± 2.41       | 23.5 ± 0.17   | 9.48×                | 288           | 0          | −100%      |
| `sequence { yieldAll(1..10); yieldAll(11..20); yield(21) }`, `toList` | 104 ± 3.73       | 102 ± 2.93    | 1.02× (within error) | 456           | 264        | −42%       |
| same, `take(10)`, `toList`                                            | 65.5 ± 0.69      | 41.9 ± 4.15   | 1.56×                | 248           | 80         | −68%       |
| same, 13 × `take(10)`, `toList`                                       | 68.1 ± 0.80      | 108 ± 2.50    | **0.63×**            | 248           | 288        | +16%       |

**Order pipeline** (`OrderPipelineBench.kt`)

This uses the data and pipeline of the `orderSequence` benchmark: 250 000 orders with 1–4 items each, of which 78%
are `COMPLETED`.

```kotlin
orders.asSequence()
    .filter { it.status == OrderStatus.COMPLETED }
    .map { order -> order.items.sumOf { it.priceCents.toLong() * it.quantity } }
    .filter { revenue -> revenue >= 10_000 }
    .map { revenue -> revenue / 100.0 }
    .map { revenue -> revenue * 1.2 }
    .take(10_000)  // omitted in the full pass
    .toList()
```

`orderSummaries` stops after the first 10 000 results, and `orderSummariesAll` drops the `take` and makes a full
pass, producing 179 687 results. A separate run checked that both variants return identical lists. These two
benchmarks use 5 forks × 10 measurement iterations (50 measurements).

| Benchmark                              | Baseline (µs/op) | Fused (µs/op)   | Speedup   | Baseline B/op | Fused B/op | Allocation |
|----------------------------------------|------------------|-----------------|-----------|---------------|------------|------------|
| `orderSummaries` (`take(10_000)`)      | 256 ± 1.08       | 102 ± 1.80      | 2.51×     | 910 104       | 408 968    | −55%       |
| `orderSummariesAll` (full pass)        | 8 204 ± 35.7     | 5 625 ± 52.3    | 1.46×     | 16 182 219    | 7 194 130  | −56%       |

**Findings**

- **The fused cost doesn't depend on chain length.** One to sixteen `map`s take 226–244 ns per 1000 elements, while
  the baseline grows from 292 ns to 48 182 ns. The fused code also allocates nothing, while the baseline allocates up
  to 237 KB per run.
- **When the output collection dominates, the gain is small.** On a `filter → map → toList` pipeline, fusion gains
  1.09×–1.44× depending on the input size, without a clear trend, and allocation is almost the same in both variants.
- **Short-circuiting consumers gain the most when they iterate far:** `firstOrNull` without a match is 6.25× faster,
  and `indexOfFirst` is 5.65× faster. One case doesn't follow: `first` with the match at the last element is 0.92×,
  although its fused loop is almost identical to the one in `firstOrNull` (279 ns). The scores are stable, so this isn't noise. The visible difference is that `first`
  takes its match branch, with an `Integer.valueOf` call, once per run.
- **Regressions:**
  - `withIndex()` is **0.34×** (1 534 vs 514 ns), and the cause is known. The fused loop unboxes each element
    (`intValue()`) and boxes it again (`Integer.valueOf`) to construct `IndexedValue`. That allocates a new `Integer`
    for every value outside the `Integer` cache: 872 elements × 16 B = the 13 952 B/op measured. The baseline passes
    the original `Integer` through. Using the element without unboxing it should fix this.
  - `drop(500)` is **0.39×** (1 096 vs. 425 ns) while allocating less. Every element passes through the per-element
    counter and its overflow check (`increment`), which might be the cause.
  - `toList()` is **0.74×** (4 794 vs. 3 556 ns) and `toMutableList()` is **0.79×** (4 738 vs. 3 739 ns), with the same
    allocation. Both are slower by about the same amount, so the cause is more likely in the fused loop than in the
    handling of empty and single-element results after it.
  - The `sequence { }` builder with 13 `take`s is **0.63×** (108 vs. 68.1 ns) and allocates 16% more (288 vs 248 B/op).
    The earlier comparison against a released compiler found the same regression (0.40×).

## Testing

The tests are box tests in `compiler/testData/codegen/box/sequence/`. Most tests check both behavior (`box()` returns
`"OK"`) and whether fusion happened, through bytecode counts:

```kotlin
// WITH_STDLIB
// CHECK_BYTECODE_TEXT
// 0 iterator          <- fused: no iterator() calls left
// 0 LOOKUPSWITCH
// 2 TABLESWITCH       <- the `when`s emitted for fused sequenceOf producers
```

Tests in `skipLowering/` assert that fusion did **not** happen, for example `// 1 iterator`.

After adding a test file, regenerate the runners and run them:

```bash
./gradlew generateTests
./gradlew :compiler:fir:fir2ir:test --tests "org.jetbrains.kotlin.test.runners.codegen.FirLightTreeBlackBoxCodegenTestGenerated\$Box\$Sequence*" -q
```

## Adding a new operation

1. **Producer or transformer:**
    1. Add an FQ-name constant and a `matchWith*` branch in `SequenceDataGatherer.visitCall`
       (`SequenceDataGathererer.kt`).
    2. Add a `SequenceSource` variant (`SequenceData.kt`) or a `SequenceTransformer` variant
       (`transformers/TransformerStrategy.kt`). For a transformer, pass the right `isMovable` and `copiedExpression`
       to `appendTransformer`.
    3. Map it in `SequenceSource.createProducerStrategy` or `TransformerStrategy.create`.
    4. Implement the strategy. Evaluate arguments once, in the stdlib's order. When wrapping, pass `skipIteration`
       through, but not `leadingDropCounts`: leaving it out resets it, which is required for anything other than
       `take` and `drop`. In `take(n).map { ... }.drop(m)`, the stdlib still iterates and calls the `map` lambda, so
       the iteration must not be skipped.
2. **Consumer:** add the FQ name to `consumerFqNames` and a branch in `createConsumerStrategy`
   (`consumers/ConsumerStrategy.kt`), then implement a `ConsumerStrategy`.
3. Check the edge cases against the stdlib implementation: empty sequences, exceptions and their messages, early
   termination, `break`/`continue`, the order in which arguments are evaluated, and the result type.
4. Add box tests with `CHECK_BYTECODE_TEXT`. Add both a fused case and a `skipLowering` case for arguments that aren't
   safe.
