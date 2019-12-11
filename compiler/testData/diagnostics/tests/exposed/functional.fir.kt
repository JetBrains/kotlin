internal class My

class Your

// Both arguments should be exposed
fun foo(my: My, f: (My) -> Unit) = f(my)

// Ok
fun bar(your: Your, f: (Your) -> Unit) = f(your)

// Exposed, returns My
fun gav(f: () -> My) = f()