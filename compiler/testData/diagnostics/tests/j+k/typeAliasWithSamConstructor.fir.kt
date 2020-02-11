// FILE: OnSubscribe.java
public interface OnSubscribe<T> {
    void f();
}

// FILE: Observable.java
public class Observable<T> {
    public Observable(OnSubscribe<T> f) {
    }
}

// FILE: Kotlin.kt

typealias ObservableAlias<T> = Observable<T>
typealias ObservableIntAlias = Observable<Int>

class A : ObservableAlias<String>({})
class B : ObservableIntAlias({})
