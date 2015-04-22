package test

inline fun bar(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) y: () -> String) = {
    call(y)
}

public inline fun <T> call(f: () -> T): T = f()