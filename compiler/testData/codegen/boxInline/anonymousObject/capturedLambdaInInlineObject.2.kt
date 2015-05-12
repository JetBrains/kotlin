package test

interface A<T> {
    fun run(): T;
}

inline fun bar(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) y: () -> String) = object : A<String> {
    override fun run() : String {
        return call(y)
    }
}

public inline fun <T> call(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) f: () -> T): T = object : A<T> {
    override fun run() : T {
        return f()
    }
}.run()