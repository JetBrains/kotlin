# Anonymous objects in inline functions. 

There are two different cases of interaction between inline functions and anonymous objects.

## Anonymous object inside inline function
```kotlin
inline fun <reified T>  foo(crossinline block: () -> Unit) {
    val simple = object {}
    val complex = object {
        fun foo() = block()
    }
    val anotherComplex = object {
        fun foo() : T? = null
    }
} 

fun callSite1() {
    foo<Int> { println("1") }
}
fun callSite2() {
    foo<String> { println("2") }
}
```

Here, we can create one class for `simple` object, but must create a class per call-site
for `complex` and `anotherComplex`. Language semantics allows us simple objects on different call-sites
be both same and different. 

JVM makes this single class as an optimization, if both functions defined in one module. 
Other backends always copy classes in such a case. 


## Anonymous object inside lambda passed to inline function

```kotlin
inline fun <T> runTwice(block: () -> T) : Pair<T, T> {
    return block() to block()
}

fun main() {
    val x = runTwice {
        object {
            fun run() { }
        }::class
    }
    require(x.first == x.second)
}
```

In that case, language semantics require us to have a single class.