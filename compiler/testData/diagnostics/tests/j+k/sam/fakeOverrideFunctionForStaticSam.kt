// FILE: BehaviorSubject.java
public class BehaviorSubject<T> extends Observable<T> {
}

// FILE: Observable.java

public class Observable<T> {
    public static <T> Observable<T> create(Observable.OnSubscribe<T> f) {
        return null;
    }
    public interface OnSubscribe<T> {
        void call(T t);
    }
}

// FILE: 1.kt
fun main() {
    BehaviorSubject.create<String>(null)
    BehaviorSubject.create<Int> { }
}