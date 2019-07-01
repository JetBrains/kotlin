// FILE: JavaClass.java

// KT-5912
class JavaClass<T> {
    public static interface Action<T> {
        void call(T t);
    }

    public static class Some<T> {
        public Some(T t) {
        }
    }

    public static interface OnSubscribe<T> extends Action<Some<T>> {}

    void perform(T t, OnSubscribe<T> subscribe) {
        subscribe.call(new Some(t));
    }
}

// FILE: 1.kt

// KT-5912
fun box(): String {
    var s = "Failt"
    JavaClass<String>().perform("") { s = "OK" }
    return s
}
