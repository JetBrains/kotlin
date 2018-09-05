package test;

import kotlin.jvm.internal.*;

public interface IntrinsicCompanionObject {
    void testInt(IntCompanionObject i);
    void testChar(CharCompanionObject c);
    void testString(StringCompanionObject s);
    void testBoolean(BooleanCompanionObject b);
    void testEnum(EnumCompanionObject e);
}
