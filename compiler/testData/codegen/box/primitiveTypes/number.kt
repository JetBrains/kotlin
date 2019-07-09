// TARGET_BACKEND: JVM

// FILE: FortyTwoExtractor.java

public class FortyTwoExtractor {
    private Number fortyTwo = new FortyTwo();

    public int intValue() {
        return fortyTwo.intValue();
    }
}

// FILE: FortyTwoExtractor.kt

class FortyTwo : Number() {
    override fun toByte() = 42.toByte()

    override fun toShort() = 42.toShort()

    override fun toInt() = 42

    override fun toLong() = 42L

    override fun toFloat() = 42.0f

    override fun toDouble() = 42.0

    override fun toChar()  = 42.toChar()    
}

fun box(): String {
    val extractor = FortyTwoExtractor()
    if (extractor.intValue() != 42) return "FAIL"
    return "OK"
}
