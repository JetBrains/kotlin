// FILE: Assert.java

public class Assert {
    public static <T> void assertThat(T actual, Matcher<? super T> matcher) {
    }
}

// FILE: Matcher.java
public class Matcher<T> {
    public static <T> Matcher<java.lang.Iterable<? super T>> hasItem(T item) {
        return null;
    }
}

// FILE: main.kt
fun test(x: List<String>) {
    Assert.<!INAPPLICABLE_CANDIDATE!>assertThat<!>(x, Matcher.hasItem("abc"))
}
