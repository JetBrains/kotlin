interface Subscriber<T>

interface Observable<T> {
    fun subscribe(s: Subscriber<in T>)
}

fun foo(o: Observable<out CharSequence>, y: Subscriber<in CharSequence>) = o.subscribe(y) // type safe

