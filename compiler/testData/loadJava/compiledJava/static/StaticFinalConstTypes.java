// SKIP_IN_RUNTIME_TEST because there's no stable way to determine if a field is initialized with a non-null value in runtime

package test;

public class StaticFinalConstTypes {
    public static final String stringField = "0";
    public static final String stringFieldNull = null;

    public static final Boolean booleanField = false;
    public static final Boolean booleanFieldNull = null;
    public static final boolean booleanPrimitiveField = false;

    public static final Character characterField = '0';
    public static final Character characterFieldNull = null;
    public static final char characterPrimitiveField = '0';

    public static final Byte byteField = (byte) 0;
    public static final Byte byteFieldNull = null;
    public static final byte bytePrimitiveField = (byte) 0;

    public static final Short shortField = (short) 0;
    public static final Short shortFieldNull = null;
    public static final short shortPrimitiveField = (short) 0;

    public static final Integer integerField = 0;
    public static final Integer integerFieldNull = null;
    public static final int integerPrimitiveField = 0;

    public static final Long longField = 0L;
    public static final Long longFieldNull = null;
    public static final long longPrimitiveField = 0L;

    public static final Float floatField = 0.0f;
    public static final Float floatFieldNull = null;
    public static final float floatPrimitiveField = 0.0f;

    public static final Double doubleField = 0.0;
    public static final Double doubleFieldNull = null;
    public static final double doublePrimitiveField = 0.0;

    public static final kotlin.UByte uByteField = null;
    public static final kotlin.UShort uShortField = null;
    public static final kotlin.UInt uIntField = null;
    public static final kotlin.ULong uLongField = null;
}
