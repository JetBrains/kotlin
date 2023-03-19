// FIR_IDENTICAL
// FILE: Ticker.java

public interface Ticker {
    String tick(String s);
}

// FILE: Tickers.java

public class Tickers {
    public static void consumeTicker(Ticker ticker) {}
}

// FILE: Selectors.java

public class Selectors {
    public static <T> T select(T a, T b) {
        return a;
    }
}

// FILE: main.kt

fun main(flag: Boolean) {
    Tickers.consumeTicker(if (flag) null else { s -> s + s })

    Tickers.consumeTicker(Selectors.select({ s -> s + s }, null))
}
