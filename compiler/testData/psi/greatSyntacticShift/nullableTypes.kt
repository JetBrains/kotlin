fun test() {
    x as? X ?: return
    x as X? ?: return

    X?::x
    X ?:: x
    X? ?:: x
    X ??:: x
    X ?? :: x

    val x: X?.() -> Unit
    val x: X??.() -> Unit
    val x: X?? .() -> Unit
    val x: X ? .() -> Unit
    val x: X ?.() -> Unit
}