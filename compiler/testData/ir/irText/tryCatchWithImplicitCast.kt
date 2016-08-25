fun testImplicitCast(a: Any) {
    if (a !is String) return

    val t: String = try { a } catch (e: Throwable) { "" }
}