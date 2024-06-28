# Inline functions can refer unavailable declarations

```kotlin
// libA
fun fooA() = 5
// libB: implementation depends on libA
inline fun fooB() = fooA()
// libC: implementation depends on libB, but not libA
fun fooC() = fooB()
```

There is a problem, while inlining `fooB` to `fooC`. 

It contains call to `fooA`, but `fooA` doesn't exist for `fooC` compilation.

For jvm it is not a problem, as it would inline `invokestatic libAKt.foo` as is, without
trying to understand what does it mean. 

For current non-jvm it is not a problem, as it has all transitive dependencies at inlining time.

Unfortunately, if trying to inline on compile time over IR it would be the same problem as in klibs.
And even worse, as jvm compilation doesn't (and can't) have IrLinker to fix this symbols later. 