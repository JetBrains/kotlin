//                               T fun (() -> T).invoke(): T
//                               │ │
fun <T> simpleRun(f: () -> T): T = f()

//         collections/List<T>
//         │
fun <T, R> List<T>.simpleMap(f: (T) -> R): R {

}

//                                              Unit
//                                              │ simpleWith.t: T
//                                              │ │ fun T.invoke(): Unit
//                                              │ │ │
fun <T> simpleWith(t: T, f: T.() -> Unit): Unit = t.f()
