package test

inline fun <reified R> foo() = bar<R>() {"OK"}
inline fun <reified E> bar(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) y: () -> String) = {
    null is E
    run(y)
}

public inline fun <T> call(f: () -> T): T = f()