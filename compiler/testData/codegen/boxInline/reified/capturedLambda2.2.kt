package test

inline fun <reified R, T> bar(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) tasksFactory: () -> T) = {
    null is R
    run(tasksFactory)
}

public inline fun <T> call(f: () -> T): T = f()

