// FILE: j/OnSubscribe.java
package j;

public interface OnSubscribe<T> {
    void f();
}

// FILE: j/Observable.java
package j;

public class Observable<T> {

    protected Observable(OnSubscribe<T> f) {
    }
}

// FILE: Kotlin.kt

import j.*

class K : Observable<String>({})