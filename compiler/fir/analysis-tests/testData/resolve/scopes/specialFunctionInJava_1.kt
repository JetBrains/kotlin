// SCOPE_DUMP: Some:toByte;byteValue;toShort;shortValue;toInt;intValue;toLong;longValue  MyNumber:toByte;byteValue

// FILE: MyBaseNumber.java
public interface MyBaseNumber {
    byte byteValue(); // (1)
    short shortValue(); // (1)
}

// FILE: MyNumber.java
public interface MyNumber extends MyBaseNumber {
    @Override
    byte byteValue(); // (2)

    @Override
    short shortValue(); // (1)

    int intValue();
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

fun test(some: Some) {
    some.toByte()
    some.toShort()
    some.toInt()
    some.toLong()

    some.byteValue()
    some.shortValue()
    some.intValue()
    some.longValue()
}
