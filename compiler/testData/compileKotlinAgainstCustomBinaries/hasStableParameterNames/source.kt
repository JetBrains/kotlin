fun test() {
    val ktInstance = KtClass()
    // Shouldn't work but is kept for backward compatibility with K1
    // When KT-4758 is fixed, this should break.
    ktInstance.foo(javaName = 1)
}
