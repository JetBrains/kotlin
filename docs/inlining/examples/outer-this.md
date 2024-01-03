### Inner classes 

The basic problematic example looks like this:

```kotlin
class A(val x: Int) {
    inner class B {
        inline fun foo() = x
    }
}

fun main() {
    A(5).B().foo()
}
```

The problem is caused by the fact that there is no field, which stores A object inside B doesn't exist after
Fir2IR. This field is added by lowering (which is now exectued before inlining, but probably should be moved after),
and is a private one. This leads to two problems
1. This field doesn't exist in klib
2. Even if it exists, it shouldn't have a public signature, so can't be referenced from other klib.

There is a possibility that we can deprecate accessing this of outer class from public inline functions.
This would make fixing things a bit easier here. 