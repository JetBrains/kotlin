# Calling inline functions from java

Non-suspend inline functions without reified parameters can be called from java. 

This is one of the places where described evolution semantics is not conformed

So original example, when called from java

```kotlin
// dependency-v1:
inline fun depFun() = "lib.v1"
// dependency-v2
inline fun depFun() = "lib.v2"
// lib: depends on dependency-v1
fun libFun() = depFun()
// Main.java: depends on lib and dependency-v2
```
```java
public class Main {
    public static void main(String[] args) {
        System.out.println(libFun());
    }
}
```

would now print `lib.v2` opposed to `liv.v1` in kotlin. 

We plan just to ignore it. 