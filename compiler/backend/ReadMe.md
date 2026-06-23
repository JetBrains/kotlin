JVM backend inliner, optimizer and [coroutine codegen](src/org/jetbrains/kotlin/codegen/coroutines/coroutines-codegen.md).

This part of the JVM backend is separated from the main modules (see [ir/backend.jvm](../ir/backend.jvm)) because it is implemented
mostly via direct manipulation of JVM bytecode via ASM.
