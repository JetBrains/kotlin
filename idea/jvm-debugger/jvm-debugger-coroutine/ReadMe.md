# Coroutine Debugger support

## Java thread stack with coroutine information
1. Real frames
1. Coroutine 'preflight' frame: [SuspendExitMode](src/org/jetbrains/kotlin/idea/debugger/coroutine/data/SuspendExitMode.kt)
is `SUSPEND_LAMBDA` or `SUSPEND_METHOD_PARAMETER`. [Location.isPreflight](org/jetbrains/kotlin/idea/debugger/coroutine/util/CoroutineUtils.kt)
1. One or more frames (skipped in debugger)
1. Coroutine 'starting' frame: [SuspendExitMode](src/org/jetbrains/kotlin/idea/debugger/coroutine/data/SuspendExitMode.kt) `SUSPEND_METHOD` frame
1. Restored from coroutine information frames
1. Real frames after the 'starting' frame
1. Creation frames (only exists if coroutine agent enabled)

## Debugger interface
Debugger works as a combination of PositionManager and AsyncStackTraceProvider. 
Once the 'preflight' signature frame found PositionManager forms a 'preflight' frame with coroutine information which gets
processed with AsyncStackTraceProvider.