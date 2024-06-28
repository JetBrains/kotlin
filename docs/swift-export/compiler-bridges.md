# Compiler bridges

This document gives an overview on how bridges between Swift wrappers and Kotlin declarations are implemented.

> ❗️ Swift export is in very early stages of development.
> Things change very quickly, and documentation might not catch up at times.

Swift does know nothing about Kotlin code, and how to call it. But Swift has a pretty good C/Objective-C interop, 
and we can exploit that by exporting Kotlin code as a C library. 

### Exporting Kotlin functions as C functions

By default, Kotlin/Native makes a few assumptions when it compiles the code:
1. All references are tracked by Kotlin/Native garbage collector and nothing else.
2. It can mangle declaration names freely.
2. Kotlin functions are not called from other binaries, and unused declarations can be removed.
3. It can use its own calling convention. For example, almost all Kotlin/Native functions have an additional parameter for a shadow stack.

This makes impossible calling Kotlin/Native binaries from the outside easily. Instead, we have to add a small C-compatible layer:
1. This layer should not pass arbitrary objects. Only special wrappers (`SpecialRef`) that are tracked by the GC.
2. Function names in this layer should be predictable.
3. Functions are excluded from dead code elimination.
4. Non-trivial function signatures are converted in a predicatable way. For example, `suspend fun foo()` are wrapped with `foo_wrapper(cont: SpecialRef)`

So for the given Kotlin function
```kotlin
package pkg

public fun foo(a: Foo): Bar { ... }
```
We can automatically create the following wrapper:
```kotlin
@CWrapper("pkg_foo_wrapper")
public fun foo_wrapper(a: SpecialRef): SpecialRef {
  val a_inner = unwrap(a)
  val result_inner = pkg.foo(a_inner)
  return wrap(result_inner)
}
```
and the corresponding C header declaration:
```c
void* pkg_foo_wrapper(void* a);
```

### Calling C from Swift

…Is pretty simple. Swift supports clang modules, so we can wrap an arbitrary C header with `module.modulemap`, and import it in Swift.

`header.h`:
```c
int foo();
```

`module.modulemap`
```
module Bridge {
    umbrella header <path-to-header>
    export *
}
```
`main.swift`
```swift
import Bridge

print(foo())
```

### Putting it all together

When Swift export encounters a Kotlin function `fun foo(a: Foo): Bar`, it creates 3 declarations:
1. Kotlin declaration
```kotlin
@CWrapper("pkg_foo_wrapper")
public fun foo_wrapper(a: SpecialRef): SpecialRef {
  val a_inner = unwrap(a)
  val result_inner = pkg.foo(a_inner)
  return wrap(result_inner)
}
```
2. C declaration
```c
void* pkg_foo_wrapper(void* a);
```
3. Swift function
```swift
import KotlinBridges
 
public func foo(a: Foo) -> Bar {
    let a_ref = convertToSpecialRef(a)
    let result_ref = pkg_foo_wrapper(a_ref)
    return convertFromSpecialRef(result_ref)
}
```

### Integration of Swift ARC and Kotlin tracing GC

Integration of memory managers is a pretty complicated topic, especially when we need to consider things like weak refs, object identity,
multithreading and so on. 
Luckily, we already solved this problem in Objective-C export. All exported Objective-C classes inherit from `KotlinBase`.
`KotlinBase` overrides `retain` and `release` implementations (yep, it is possible in Objective-C export thanks to its dynamic nature!), and
calls Kotlin/Native GC operations under the hood. But what should we do in Swift export, where such overrides are not possible?

Luckily for us, Swift allows inheriting from Objective-C classes! 
This allows us to do an amazing thing: reuse existing integration of memory managers by inheriting 
all generated Swift classes from the same old `KotlinBase` (with a few adjustments), and avoid solving the same problems over again.

We might consider implementing another integration scheme in the future, but at the current state of development, 
this approach is sufficient. 