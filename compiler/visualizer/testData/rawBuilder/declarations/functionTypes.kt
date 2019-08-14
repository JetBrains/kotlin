//                                        fun ((T) -> Unit).invoke(T): Unit
//                                        │
fun <T> simpleRun(f: (T) -> Unit): Unit = f()

//         collections/List<T>
//         │
fun <T, R> List<T>.simpleMap(f: (T) -> R): R {

}

//                                                simpleWith.t: T
//                                                │ fun T.invoke(): Unit
//                                                │ │
fun <T> simpleWith(t: T, f: T.() -> Unit): Unit = t.f()

