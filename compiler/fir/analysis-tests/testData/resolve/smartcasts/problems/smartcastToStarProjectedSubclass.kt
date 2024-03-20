// WITH_STDLIB
// ISSUE: KT-56722
// FILE: Option.java
public interface Option<T> {
    T get();

    public final class Some<T> implements Option<T> {
        @Override
        public T get() {
            return null;
        }
    }
}

// FILE: test.kt
fun test_1(option: Option<Pair<String, String>>?) {
    if (option is Option.Some<*>) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("CapturedType(*)!! & kotlin.Pair<kotlin.String, kotlin.String>..CapturedType(*)? & kotlin.Pair<kotlin.String, kotlin.String>?")!>option.get()<!>.first
        x.length
    }
}

fun test_2(option: Option<Pair<String, String>>?) {
    if (option is Option.Some) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Pair<kotlin.String, kotlin.String>..kotlin.Pair<kotlin.String, kotlin.String>?!")!>option.get()<!>.first
        x.length
    }
}
