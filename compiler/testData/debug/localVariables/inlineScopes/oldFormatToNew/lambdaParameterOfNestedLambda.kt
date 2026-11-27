// WITH_STDLIB
// MODULE: library
// FILE: library.kt
// KT-91573: the parameter of the inner `let` lambda belongs to that lambda, not to the outer `forEach2` lambda,
// although both have the same inline depth in the old format.

inline fun <T> List<T>.forEach2(action: (T) -> Unit) {
    for (element in this) action(element)
}

inline fun <T, R : Any> List<T>.mapNotNull2(transform: (T) -> R?): List<R> {
    val destination = ArrayList<R>()
    forEach2 { element ->
        transform(element)?.let {
            destination.add(it)
        }
    }
    return destination
}

// MODULE: test(library)
// USE_INLINE_SCOPES_NUMBERS
// FILE: test.kt

fun box() {
    listOf("a").mapNotNull2 { s -> s.length }
}

// EXPECTATIONS JVM
// test.kt:26 box:
// library.kt:12 box: $this$mapNotNull2\1:java.util.List=java.util.Collections$SingletonList, $i$f$mapNotNull2\1\26:int=0:int
// library.kt:13 box: $this$mapNotNull2\1:java.util.List=java.util.Collections$SingletonList, $i$f$mapNotNull2\1\26:int=0:int, destination\1:java.util.ArrayList=java.util.ArrayList
// library.kt:8 box: $this$mapNotNull2\1:java.util.List=java.util.Collections$SingletonList, $i$f$mapNotNull2\1\26:int=0:int, destination\1:java.util.ArrayList=java.util.ArrayList, $this$forEach2\2:java.util.List=java.util.Collections$SingletonList, $i$f$forEach2\2\47:int=0:int
// library.kt:14 box: $this$mapNotNull2\1:java.util.List=java.util.Collections$SingletonList, $i$f$mapNotNull2\1\26:int=0:int, destination\1:java.util.ArrayList=java.util.ArrayList, $this$forEach2\2:java.util.List=java.util.Collections$SingletonList, $i$f$forEach2\2\47:int=0:int, element\2:java.lang.Object="a":java.lang.String, element\3:java.lang.Object="a":java.lang.String, $i$a$-forEach2-LibraryKt$mapNotNull2$1\3\48\1:int=0:int
// test.kt:26 box: $this$mapNotNull2\1:java.util.List=java.util.Collections$SingletonList, $i$f$mapNotNull2\1\26:int=0:int, destination\1:java.util.ArrayList=java.util.ArrayList, $this$forEach2\2:java.util.List=java.util.Collections$SingletonList, $i$f$forEach2\2\47:int=0:int, element\2:java.lang.Object="a":java.lang.String, element\3:java.lang.Object="a":java.lang.String, $i$a$-forEach2-LibraryKt$mapNotNull2$1\3\48\1:int=0:int, s\5:java.lang.String="a":java.lang.String, $i$a$-mapNotNull2-TestKt$box$1\5\54\0:int=0:int
// library.kt:14 box: $this$mapNotNull2\1:java.util.List=java.util.Collections$SingletonList, $i$f$mapNotNull2\1\26:int=0:int, destination\1:java.util.ArrayList=java.util.ArrayList, $this$forEach2\2:java.util.List=java.util.Collections$SingletonList, $i$f$forEach2\2\47:int=0:int, element\2:java.lang.Object="a":java.lang.String, element\3:java.lang.Object="a":java.lang.String, $i$a$-forEach2-LibraryKt$mapNotNull2$1\3\48\1:int=0:int
// library.kt:15 box: $this$mapNotNull2\1:java.util.List=java.util.Collections$SingletonList, $i$f$mapNotNull2\1\26:int=0:int, destination\1:java.util.ArrayList=java.util.ArrayList, $this$forEach2\2:java.util.List=java.util.Collections$SingletonList, $i$f$forEach2\2\47:int=0:int, element\2:java.lang.Object="a":java.lang.String, element\3:java.lang.Object="a":java.lang.String, $i$a$-forEach2-LibraryKt$mapNotNull2$1\3\48\1:int=0:int, it\4:java.lang.Object=java.lang.Integer, $i$a$-let-LibraryKt$mapNotNull2$1$1\4\54\3:int=0:int
// library.kt:16 box: $this$mapNotNull2\1:java.util.List=java.util.Collections$SingletonList, $i$f$mapNotNull2\1\26:int=0:int, destination\1:java.util.ArrayList=java.util.ArrayList, $this$forEach2\2:java.util.List=java.util.Collections$SingletonList, $i$f$forEach2\2\47:int=0:int, element\2:java.lang.Object="a":java.lang.String, element\3:java.lang.Object="a":java.lang.String, $i$a$-forEach2-LibraryKt$mapNotNull2$1\3\48\1:int=0:int, it\4:java.lang.Object=java.lang.Integer, $i$a$-let-LibraryKt$mapNotNull2$1$1\4\54\3:int=0:int
// library.kt:14 box: $this$mapNotNull2\1:java.util.List=java.util.Collections$SingletonList, $i$f$mapNotNull2\1\26:int=0:int, destination\1:java.util.ArrayList=java.util.ArrayList, $this$forEach2\2:java.util.List=java.util.Collections$SingletonList, $i$f$forEach2\2\47:int=0:int, element\2:java.lang.Object="a":java.lang.String, element\3:java.lang.Object="a":java.lang.String, $i$a$-forEach2-LibraryKt$mapNotNull2$1\3\48\1:int=0:int
// library.kt:17 box: $this$mapNotNull2\1:java.util.List=java.util.Collections$SingletonList, $i$f$mapNotNull2\1\26:int=0:int, destination\1:java.util.ArrayList=java.util.ArrayList, $this$forEach2\2:java.util.List=java.util.Collections$SingletonList, $i$f$forEach2\2\47:int=0:int, element\2:java.lang.Object="a":java.lang.String, element\3:java.lang.Object="a":java.lang.String, $i$a$-forEach2-LibraryKt$mapNotNull2$1\3\48\1:int=0:int
// library.kt:8 box: $this$mapNotNull2\1:java.util.List=java.util.Collections$SingletonList, $i$f$mapNotNull2\1\26:int=0:int, destination\1:java.util.ArrayList=java.util.ArrayList, $this$forEach2\2:java.util.List=java.util.Collections$SingletonList, $i$f$forEach2\2\47:int=0:int, element\2:java.lang.Object="a":java.lang.String
// library.kt:9 box: $this$mapNotNull2\1:java.util.List=java.util.Collections$SingletonList, $i$f$mapNotNull2\1\26:int=0:int, destination\1:java.util.ArrayList=java.util.ArrayList, $this$forEach2\2:java.util.List=java.util.Collections$SingletonList, $i$f$forEach2\2\47:int=0:int
// library.kt:18 box: $this$mapNotNull2\1:java.util.List=java.util.Collections$SingletonList, $i$f$mapNotNull2\1\26:int=0:int, destination\1:java.util.ArrayList=java.util.ArrayList
// test.kt:27 box:
