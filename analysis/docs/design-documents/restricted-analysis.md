# Dumb Mode Support (Restricted Analysis)

**Last update:** February 12, 2025

**Issue:** [KT-74090](https://youtrack.jetbrains.com/issue/KT-74090) Analysis API: Support dumb mode

**Contributors:** Marco Pennekamp, Yan Zhulanow

This is a design document describing how to support IntelliJ’s [dumb mode](https://plugins.jetbrains.com/docs/intellij/indexing-and-psi-stubs.html#dumb-mode) in the Analysis API.

## Historical context & motivation

At its inception, the Analysis API was designed without explicit dumb mode support. While it was possible to access the Analysis API during dumb mode, it was implicitly assumed that such accesses were illegal. The underlying reason was that the Analysis API heavily relies on indices and a sophisticated caching infrastructure.

Still, some components of the IntelliJ Kotlin plugin accessed the Analysis API during dumb mode, especially via light classes. As the Analysis API wasn’t designed to be used during dumb mode, dumb mode access to the Analysis API was explicitly forbidden with an exception ([KT-63490](https://youtrack.jetbrains.com/issue/KT-63490)).

In recent times, IntelliJ has pushed for the support of more and more functionality during dumb mode, in a general effort to improve the user experience during indexing. Dumb mode support for the Analysis API is highly requested to follow this trend.

As such, we decided to add dumb mode support to the Analysis API.

## Challenges & goals

Dumb mode support has the following main challenges:

* **Index access:** The Analysis API relies heavily on indices to perform even basic resolution, as symbol providers access indices to provide symbols from sources and libraries. Traditionally, index access was prohibited during dumb mode, resulting in `IndexNotReadyException`s, but nowadays it is possible to access only “reliable data.” More on that later.
* **Lack of consistency:** The Analysis API operates in an immutable project context. Analysis can only be performed in read actions, where code or project structure modification is not possible. Caches are only invalidated when no analysis is performed. In contrast, indices change their state during dumb mode as indexing progresses, even during read actions. As indices are often the source of truth for LL FIR, their changing state introduces mutability into a usually immutable context.
* **Lack of tests in dumb mode:** IntelliJ has very few tests which specifically test features during dumb mode. Our performance tests, which are also used as correctness tests, are equally not designed to be executed in dumb mode. Hence, it is difficult to gauge whether the Analysis API works correctly in dumb mode without manual testing or adding additional tests.

There are two dimensions to “correct” dumb mode support:

* **Correctness of results:** This concerns whether the results that the Analysis API produces are correct. 100% correctness cannot be achieved in a project which is not fully indexed, and IntelliJ features are aware of this. Nonetheless, we need to consider correctness as there is certainly a bar where the usefulness of the Analysis API degrades markedly due to too many wrong results.
* **Exceptions:** Inconsistencies in the cache state may cause Analysis API and FIR frontend assertions to be triggered. Such assertions typically detect compiler/lazy resolution bugs, but they assume a complete, unchanging project state.

The **main risk** of naive dumb mode support (without any mitigations) is the risk of exceptions which are difficult to diagnose and (almost) impossible to reproduce. Especially, exceptions from dumb mode should not mix with exceptions from smart mode in exception aggregation tools, as we might otherwise misdiagnose dumb mode exceptions as happening generally in IntelliJ.

However, the lack of tests makes it difficult to ensure that we have a comprehensive picture of the dumb mode support’s quality.

As such, the first implementation of dumb mode support has the following **goals:**

* All **Analysis API functionality** should be allowed in dumb mode. We might disable or simplify certain features if we get too many exceptions for them, but that requires the data first.
* We should **monitor exceptions** from dumb mode to gauge their impact on the user experience. Dumb mode exceptions should be separately classifiable in exception aggregation tools.
* We should **focus on exceptions over the correctness of results**. The latter is flexible and issues with result correctness can be discovered on a feature-by-feature basis, but exceptions are disruptive to the user experience and cause features to stop working unexpectedly.
* Due to the lack of tests, certain **quality gates** must be passed before the feature can be enabled for the general population.

## Abstracting over “dumb mode” – Restricted Analysis

While the Analysis API is currently tied to the IntelliJ platform and has access to `DumbService`, it’s generally a good idea to decouple Analysis API concepts from the IntelliJ platform as much as possible. This gives a fair chance to other Analysis API platforms to implement their own conception of a “dumb mode” without having to hack around the IntelliJ platform.

Hence, the Analysis API’s version of “dumb mode” is called **restricted analysis mode**. The exact meaning of “restricted” is up to the platform.

We define the following terms:

* **Restricted analysis mode:** Analysis is currently restricted, e.g. dumb mode is enabled. The “mode” refers to the period of time during which restricted analysis might occur, not the restricted analysis itself.
* **Restricted analysis:** This refers to analysis performed during restricted analysis mode. In contrast to the mode, “restricted analysis” is a term for the analysis activity itself.

The optional `KotlinRestrictedAnalysisService` platform component will be added to allow the Analysis API platform to communicate whether analysis is currently restricted and whether restricted analysis is allowed.

This document describes both the behavior of the Analysis API under restricted analysis mode, as well as the changes that have to be made to the IntelliJ implementation. The Standalone Analysis API is not implementing any restricted analysis functionality at this time.

## Analysis API behavior

### Exception wrapping

To allow proper classification of exceptions that occurred during restricted analysis in exception aggregation tools, the Analysis API wraps all exceptions passing through `analyze` in a `KaRestrictedAnalysisException` during restricted analysis mode.

Even though `analyze` is an inline function, we can extend the existing try-finally in `KaSessionProvider` to wrap exceptions. Clients will have to recompile their `analyze` calls to benefit from the wrapping, but this is fine since the absence of wrapping doesn’t break anything. In particular, binary compatibility is not broken because the previous version of the `analyze` function will still work.

To avoid additional edits to `analyze`, the `catch` should simply call a function `handleThrowable` which does the wrapping. It should not be performed directly in `analyze`.

A test should be added for the exception wrapping, for example by mocking `KotlinRestrictedAnalysisService` and throwing an exception from the `analyze` block.

## Platform-specific implementation

The IntelliJ implementation for `KotlinRestrictedAnalysisService` reports whether dumb mode is currently active.

### Index access with reliable data

In IntelliJ, by default, indices cannot be accessed during dumb mode. It’s required to use a special `DumbModeAccessType` to access indices. The only access type which fits our needs is `RELIABLE_DATA_ONLY`, which at any given moment only exposes index data from files which have been fully indexed and are not marked as dirty.

It’s important to note that even with `RELIABLE_DATA_ONLY`, the index state will change as files are reindexed, even during a read action. This is the consistency issue mentioned in the introductory section. For now, we will ignore potential consistency issues. However, strategies for improving cache consistency are outlined at the bottom of the document.

We have two options for implementing “reliable data” access:

1. The declaration provider, package provider, and other components of the Analysis API platform are responsible for accessing indexes correctly during restricted analysis mode. In IntelliJ, this means that components like declaration providers need to wrap their index access in `RELIABLE_DATA_ONLY.ignoreDumbMode`. Any index access reachable from the Analysis API should then be wrapped like this.
2. `KotlinRestrictedAnalysisService` could provide a function `withReliableDataAccess` which essentially exposes `DumbModeAccessType.ignoreDumbMode` to the Analysis API. This would allow the Analysis API to wrap call sites where we expect index access and lift the burden off the Analysis API platform implementation. However, it’s questionable whether other platform implementations would follow the same strategy. Furthermore, it’s much easier to ensure proper coverage of “reliable data” access when we do it as close to the index access as possible.
    1. Alternatively, `analyze` could wrap the whole block in `withReliableDataAccess`, but this would further complicate the implementation of `analyze`. The change would also not be binary-compatible, as proper restricted analysis support would require clients to recompile usages of `analyze`. And finally, the wrapping might be unnecessary if no index access is performed (for example when all symbols and resolution results are already cached).

Given the complications with the second option, the first option is currently the best one.

### Cache invalidation

To isolate the effects of restricted analysis from regular analysis, Analysis API caches must be fully invalidated upon **exit from restricted analysis mode**. We cannot rely on the state of any session whose information might have been built on incomplete indices, so even if there is no further modification upon exiting restricted analysis mode, we should invalidate such sessions.

This is the responsibility of the Analysis API platform. The platform should publish a global module state modification event to invalidate all caches. In IntelliJ, `InvalidateCachesAfterDumbMode` already handles this, but it needs to be lifted out of its workaround status. (If the additional write action causes freezes, we might be able to replace the global modification event with stop-the-world session invalidation, although this approach disregards other listeners of modification events. But any write action-less solution is likely premature and should only be considered if there are actual freeze reports.)

While it’s theoretically possible to invalidate only sessions which were affected by unindexed files, this is technically too complicated, as dirty files might even be added to the set of files to index during dumb mode (e.g. after another modification). Hence, it is easier to invalidate all caches.

Similarly, any **entry to restricted analysis mode** should start with fresh caches for all sessions that may be affected by unindexed files. For the time being, in IntelliJ, we will rely on the fact that dumb mode starts in a write action with the modification itself. Every modification which might affect Analysis API caches should result in a modification event, so active session invalidation should already ensure that we enter restricted analysis mode with fresh caches for affected sessions.

The exception might be **application start-up:** IntelliJ starts in smart mode and scans files at the same time. Here, indices can access stale data. When a lot of file changes are discovered, dumb mode is started. However, at this point we don’t have a modification event which we can rely on, so we might need to invalidate caches explicitly when entering dumb mode on start-up.

### Feature flag

To mitigate the risks outlined above, the Kotlin plugin introduces a feature flag `kotlin.analysis.allowRestrictedAnalysis`. When the flag is enabled, restricted analysis as outlined in this document is allowed. When the flag is disabled, the previous Analysis API behavior is restored where an `IndexNotReadyException` is thrown when analysis is restricted.

### Dumb mode tests

On the Kotlin plugin side, we can add dumb mode tests which access an index with partially reliable data. In other words, some of the files of the index are consistently not part of the reliable data set. This would allow us to write a dumb mode test which doesn’t suffer from timing issues around active reindexing.

While test infrastructure for “partial index tests” doesn’t exist yet, it would be possible to implement such test infrastructure. As a (perhaps simpler) alternative, a test can mark specific files as dirty without triggering reindexing. This might achieve the same state where a few predefined files aren’t part of the reliable data set.

Dumb mode tests could also possibly be adapted from the Java language support.

While it would be great to achieve full test coverage of all features which can work in dumb mode, even having just *a few* dumb mode tests would be greatly beneficial as a sort of smoke test. A little is much, much better than nothing. That said, it is a non-goal of this proposal to scope out the exact tests that would need to be created.

### Exception suppression

`KaRestrictedAnalysisException`s occur frequently, we can consider suppressing them in IntelliJ. If the quality gates aren’t met in reasonable time, suppressing exceptions might be a valid alternative to keeping the feature disabled for external users.

## Quality criteria

The following quality criteria need to be assessed before the feature can be enabled by default:

* **Exceptions:** The exceptions from restricted analysis must be at an acceptable level. The exact threshold hasn’t been defined, but it depends on both the number of exceptions and the nature of the exceptions.
    * To gauge the impact of the restricted analysis mode, we’ll monitor `KaRestrictedAnalysisException`s in exception aggregation tools. For every (popular) exception, we’ll try to investigate whether the exception needs to be fixed. The total number of exceptions will give a picture whether we need to take additional measures to ensure cache consistency (outlined further below).
* **Tests:** At least a few dumb mode tests should be added on the Kotlin plugin side.
* **Manual QA:** We should manually test IntelliJ with perpetual dumb mode enabled. While indices are unlikely to be in an inconsistent state during such testing, it might uncover other problems with the dumb mode support.

## Cache consistency during restricted analysis

As mentioned above, we will forego measures to improve cache consistency at first. This is due to the complexity of implementing such measures. However, as various possibilities have already been considered and discussed, we want to outline them in this section.

That said, we can decide later to implement consistency improvements if the need arises.

### Problem

As explained in the “Challenges” section, we have the following consistency challenge:

“The Analysis API operates in an immutable project context. Analysis can only be performed in read actions, where code or project structure modification is not possible. Caches are only invalidated when no analysis is performed. In contrast, **indices change their state during dumb mode as indexing progresses**, even during read actions. As indices are often the source of truth for LL FIR, their **changing state introduces mutability into a usually immutable context**.”

Consider the following example:

```kotlin
// File: A.kt (needs to be reindexed)
class A

// File: foo.kt (fully indexed)
fun foo1(): A = A() // Resolved *before* `A.kt` has been reindexed.
//          ^ error

fun foo2(): A = A() // Resolved *after* `A.kt` has been reindexed.
//          ^ valid
```

We could run into a cache state where the usage of `A` in `foo1` is an error, but the usage in `foo2` is valid. It’s unclear whether this will only result in incorrect answers from the Analysis API or even exceptions when consistency assertions are triggered in the frontend/LL FIR.

To summarize the problem:

* Cached information may contradict other cached information.
* We are uncertain whether this will lead to additional exceptions from consistency assertions.

### Invalidation-based approach

If we can hook into events from indexing as files are reindexed, we might be able to invalidate caches at the right moment to ensure cache consistency or at least mitigate the risk of inconsistent caches somewhat.

IntelliJ offers a modification tracker for stub indices: `StubIndex#getStubIndexModificationTracker`, which is the associated tracker for an implementation of an index update event.

However, even if we can get such an event during dumb mode, indices are updated constantly. This means that such an event would be fired constantly, and probably outside a write action. If it’s fired outside a write action, we can only perform session invalidation with stop-the-world session invalidation. In the worst case, which might actually be quite common if indexing events are fired fast enough, analysis will stall completely as we’re queueing stop-the-world session invalidations after one another.

Furthermore, given that index updates can happen inside read actions as well, an invalidation-based solution would actually not solve the issue, because the index update event will be fired after the index has already been updated. If analysis is running at the time, it’ll see the updated index midway through. Even worse, stop-the-world session invalidation will wait for these analysis calls to finish (with the already updated index), inviting the same consistency exceptions that we want to avoid.

Hence, an invalidation-based approach is not a viable solution.

### Scope-based approach

To avoid an inconsistent cache state, we could pin the files that can be accessed by declaration providers to the start of the dumb mode. We’d create a scope which covers all the files that were invalidated and need to be reindexed. Even if a file has been reindexed and is available again, it will be excluded by the scope from declaration providers until dumb mode ends. Hence, the Analysis API would have a stable view on the project.

While it’s a great idea, the solution has a few problems when it comes to the details:

* While we can treat declarations from a file as non-existing, the file can still be opened in the IDE. As the Analysis API generally strives to be able to provide a `KaSymbol` for a given (analyzable) PSI declaration, we might run into additional consistency errors when it’s not possible to provide a `KaSymbol` for a declaration from an excluded file, or to ultimately resolve a FIR symbol for a PSI-based `KaSymbol`.
    * Since we exclude the files only from the declaration provider’s scope, but not the content scope of the module, such PSI declarations would definitely still be seen as analyzable.
    * To mitigate this problem, we can introduce file-based declaration providers for all excluded files. However, this moves the issue from the project level (one project-wide exclusion scope) to the module level (file-based declaration providers *per module*). While we can easily get a set of dirty files, associating them with `KaModule`s might be costly, or even difficult/error-prone (since the files are currently dirty).
    * Furthermore, scaling file-based declaration providers with a large number of excluded files might hit memory or performance limits at some point.
* While it’s currently possible to retrieve a list of dirty files, it might change in the future.
* During dumb mode, modification events still occur. While cache invalidation will invalidate affected caches, we'd also need to update the project-wide set of excluded files. It is not a huge issue to add modification event-based invalidation here, but it makes the solution more complex.
* Additional files may be marked as dirty during dumb mode, so the set of excluded files would need to *grow*. If the change caused a modification event, we may be able to rely on our invalidation mechanisms. But in any case, this adds complexity.

In summary, the solution has significant hurdles which might even cause more exceptions or performance issues than cache inconsistencies would.

### Timestamp-based approach

If we can get a timestamp of when an index entry was last reindexed, we can discard any results that were updated after dumb mode started. This would allow us to pin the cache state to a *temporal* point before dumb mode started (as opposed to a *spatial* point with the scope-based approach).

This idea is nice, but doesn’t match with the contracts guaranteed by indices. Conceptually, each virtual file has its own modification counter which acts somewhat like a timestamp. But we’d have to remember the modification count of every single virtual file before dumb mode starts for comparison, which is obviously impossible.

Furthermore, the solution also suffers from the same issue as the scope-based solution: the Analysis API wants to allow building a `KaSymbol` for every PSI declaration in the scope of a context module. If the PSI declaration is not contained in indices, we might not be able to provide a FIR symbol for it, or run into other consistency exceptions.