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
