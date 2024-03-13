// SCOPE_DUMP: Some:toByte;byteValue MyNumber:toByte;byteValue
// -SCOPE_DUMP: Some:toInt;intValue;toByte;byteValue;toLong;longValue

// FILE: MyBaseNumber.java
public interface MyBaseNumber {
    byte byteValue(); // (1)
}

// FILE: MyNumber.java
public interface MyNumber extends MyBaseNumber {
    @Override
    byte byteValue(); // (2)
}

// FILE: Some.java
public abstract class Some extends Number implements MyNumber {
    @Override
    public abstract byte byteValue() // (3)
}

/*
 * Some.toByte() (3', renamed) overrides Number.toByte & MyNumber.toByte(2', renamed)
 * MyNumber.toByte(2', renamed) overrides ???
 */
// FILE: main.kt
fun test(some: Some) {
    some.toByte()
}
