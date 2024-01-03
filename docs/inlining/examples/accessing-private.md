# Accessing private declarations from inline functions

Sometimes, inline functions have more priveliges, than their callsite has. 

```kotlin
class A {
    private fun bar() { println("bar")}
    inline internal fun foo() = bar()
}

fun main() {
    A().foo()
}
```

This code is compilable, and effectively equivalent to calling `bar()` in main. 
But bar() is a private function and can't be called in main().

### Current non-jvm approach

As inlining happens after klib linking, there is no issue with that. 

### Current jvm-approach

For methods, synthetic accessors are generated. 
For example, class `A` from above is generated to 

```
public final class A {
  public A();
  private final void bar();
  public final void foo$main();
  public static final void access$bar(A);
}
```

And this `access$bar(A)` method is called instead of `bar` inside inline functions. 

Accessing classes is more complex. They are just accessed as is. Which works in simple cases, but for example

```kotlin
// other.kt
package other

class A {
    private class B public constructor() {
        fun bar() { println("bar")}
    }

    private fun produce() : Any = B()
    private fun consume(b: B) { b.bar() }
    inline internal fun foo() {
        val x = produce() as B
        consume(x)
    }
}
// main.kt
fun main() {
    other.A().foo()
}
```

fails with IllegalAccessError. 

It is unclear if it should be considered as a bug, and deprecated, or it is intended behaviour. 

Probably, we can use this approach in all backends and move it to fir2ir phase.
