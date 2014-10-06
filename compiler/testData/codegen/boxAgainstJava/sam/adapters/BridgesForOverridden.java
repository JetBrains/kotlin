class JavaClass {
    public interface Super1<T> {
        Thread call(T t);
    }

    public interface Super2<T> {
        T call(String s);
    }

    public interface Sub extends Super1<String>, Super2<Thread> {
        Thread call(String s);
    }

    static void samAdapter(Sub sub) {
        ((Super1) sub).call("");
        ((Super2) sub).call("");
        sub.call("");
    }
}
