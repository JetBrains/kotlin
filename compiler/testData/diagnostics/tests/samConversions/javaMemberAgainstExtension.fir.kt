// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

// FILE: Observer.java
public interface Observer<K> {
    void onChanged(K k);
}

// FILE: LiveData.java
public class LiveData<T> {
    public void observe(java.lang.Runnable r, Observer<? super T> o) {}
}

// FILE: extension.kt
fun <T> LiveData<T>.observe(a: Any, observer: (T) -> Unit): Observer<T> {
    return Observer { observer(it) }
}

// FILE: test.kt
fun <T> test1(r: Runnable, l: LiveData<T>): Observer<T> = <!RETURN_TYPE_MISMATCH, TYPE_MISMATCH!>l.observe(r) {  }<!> // partial conversion

fun <T> test2(r: Runnable, o: Observer<T>, l: LiveData<T>) {
    val a = l.observe(r, o) // no conversion
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>a<!>

    val b = l.observe({}, {}) // conversion for all arguments
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>b<!>

    val c = l.observe({}) {} // conversion for all arguments
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>c<!>
}
