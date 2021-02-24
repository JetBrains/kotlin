// ISSUE: KT-41940

// FILE: Matcher.java

public interface Matcher<T> {}

// FILE: Matchers.java

public class Matchers {
    public static <T> Matcher<java.lang.Iterable<? super T>> hasItem(T item) {
        return null;
    }

    public static <T> void assertThat(T actual, Matcher<? super T> matcher) {}
}

// FILE: main.kt

import Matchers.*

fun test(list: List<String>, string: String) {
    assertThat(list, <!DEBUG_INFO_EXPRESSION_TYPE("Matcher<kotlin.collections.MutableIterable<*>..kotlin.collections.Iterable<*>?!>..Matcher<kotlin.collections.MutableIterable<*>..kotlin.collections.Iterable<*>?!>?!")!>hasItem(string)<!>)
}
