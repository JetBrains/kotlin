fun nothingFoo() = throw Exception()

fun<T> genericFoo1(t: T): T = t

fun<T: Any> genericFoo2(t: T): T? = t

fun<T: Runnable> genericFoo3(t: T): T = t

fun foo(): Runnable {
    return <caret>
}

// ABSENT: nothingFoo
// ABSENT: genericFoo1
// ABSENT: genericFoo2
// EXIST: { itemText: "genericFoo3", tailText: "(t: Runnable) (<root>)", typeText: "Runnable" }
