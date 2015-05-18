class Test<T> {
    public static interface Action<T> {
        void call(T t, String name);
    }

    void perform(T t, Action<T> subscribe) {
        subscribe.call(t, "");
    }
}