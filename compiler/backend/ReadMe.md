Old (classic) JVM backend.

It is not used by default since kotlin 1.5, and is being removed ([KT-71197](https://youtrack.jetbrains.com/issue/KT-71197/Remove-old-JVM-backend-code-and-tests)).

However, some code there is also being used by the [new JVM backend](../ir/backend.jvm), mainly: bytecode inliner, bytecode optimizations, [coroutine codegen](src/org/jetbrains/kotlin/codegen/coroutines/coroutines-codegen.md).