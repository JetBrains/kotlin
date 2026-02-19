// DO_NOT_CHECK_SYMBOL_RESTORE_K1
// DO_NOT_CHECK_SYMBOL_RESTORE_K2
// FILE: main.kt
data class WrapInt(val p: Int)

data class WrapFloat(val p: Float)

fun <T : Any> test(conf: Config<T>, other: Int) {
    when (other) {
        0 -> {
            conf as Config<Int>
            WrapInt(conf.min<caret>Value ?: Int.MIN_VALUE)
        }
        else -> {
            conf as Config<Float>
            WrapFloat(conf.minValue ?: Float.MIN_VALUE)
        }
    }
}

// FILE: Config.java
public class Config<T> {
    public final T minValue;

    public T getMinValue() {
        return minValue;
    }
}