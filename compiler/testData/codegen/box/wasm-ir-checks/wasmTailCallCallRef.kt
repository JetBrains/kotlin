// TARGET_BACKEND: WASM
// ENABLE_TAIL_CALLS

// Stress test for tail-call emission on the `callRef` (indirect / closure) dispatch path.
// Each level of recursion goes through a callable-reference `invoke` bridge whose body is
// `return callRef(this.func, ...)`. Unless that callRef is emitted as `return_call_ref`, every
// level keeps the invoke frame and the recursion overflows the host stack at depth 1_000_000.
//
// The result type is `Any` on purpose: the generic `Function*.invoke` is erased to return
// `kotlin.Any`, so the surrounding calls only stay in tail position (and the chain only runs in
// constant stack) when the enclosing functions also flow `Any`. This isolates the callRef
// emission from the separate question of Unit/primitive return-type erasure.

// Recursion through a freshly-allocated closure on every level.
fun closureCountdown(n: Int): Any {
    if (n == 0) return 0
    val step: () -> Any = { closureCountdown(n - 1) }
    return step()
}

// Recursion through a bound function reference.
fun refTarget(n: Int): Any = if (n == 0) 0 else refCountdown(n - 1)

fun refCountdown(n: Int): Any {
    val step: (Int) -> Any = ::refTarget
    return step(n)
}

fun box(): String {
    val depth = 1_000_000

    if (closureCountdown(depth) != 0) return "fail closure"
    if (refCountdown(depth) != 0) return "fail ref"

    return "OK"
}
