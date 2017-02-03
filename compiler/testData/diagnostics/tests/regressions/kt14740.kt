// FILE: Foo.java
public abstract class Foo {

    public interface Transformer<T, R> {}
    public interface LifecycleTransformer<T> extends Transformer<T, T> {}

    public class Observable<T> {
        public <R> Observable<R> compose(Transformer<? super T, ? extends R> transformer) {
            return null;
        }
    }


    public final <T> LifecycleTransformer<T> bindToLifecycle() {
        return null;
    }
}

// FILE: 1.kt
fun <T> Foo.Observable<T>.bindTo(a: Foo): Foo.Observable<T>  = compose(a.bindToLifecycle())