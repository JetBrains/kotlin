package test

inline fun <reified R, T> bar(crossinline tasksFactory: () -> T) = {
    null is R
    run(tasksFactory)
}

public inline fun <T> call(f: () -> T): T = f()

