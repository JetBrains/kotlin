// FIR_IDENTICAL
// SKIP_TXT
// FILE: main.kt

sealed class ClientBootResult

object ClientBootSuccess : ClientBootResult()

fun example(): Single<out ClientBootResult> {
    return Single.just(true).map<ClientBootResult> { ClientBootSuccess }
}

// FILE: Single.java
import io.reactivex.rxjava3.annotations.NonNull;

public class Single<@NonNull T> {
    @NonNull
    public static <@NonNull T> Single<T> just(T item) {
        return null;
    }
    @NonNull
    public final <@NonNull R> Single<R> map(@NonNull Function<? super T, ? extends R> mapper) {
        return null;
    }
}

// FILE: Function.java
import io.reactivex.rxjava3.annotations.NonNull;

@FunctionalInterface
public interface Function<@NonNull T, @NonNull R> {
    R apply(T t) throws Throwable;
}