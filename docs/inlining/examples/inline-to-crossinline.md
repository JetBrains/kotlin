# Why is JVM model simpler than klib? 

Inline function have several more dimensions of declaration changes, compared to normal ones.
- Type parameter can be converted between reified and not
- Lambda paramerets can be inline/noinline/crossinline
- inline keyword itself can be added/removed

In klib compatibility model, we need to answer what happens in all these cases.

In jvm compatibility mode, there are effectively no inline functions on link-time
(except corner cases like [calling from java](calling-from-java.md) and [inline override](inline-override.md)).
, but in that cases, inline functions already behaves as normal ones.
So they can't be changed, and this makes mental model much easier.  

For example, what this non-local return even mean, when `l` is `noinline`?

```kotlin
// MODULE: lib
// FILE: lib.kt
// version: v1
inline fun foo(l: () -> Unit) {
    l()
}
// version: v2
inline fun foo(noinline l: () -> Unit) {
    l()
}

// MODULE: other
// FILE: other.kt
// compile("lib:v1")
fun test() {
    foo {
        return
    }
}


// MODULE: app
// compile("other")
// compile("lib:v2")
fun main() {
    test() // Will it link?
}
```

