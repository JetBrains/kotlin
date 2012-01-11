package org.jetbrains.jet.lang.types;

/**
 * @author Stepan Koltsov
 */
public enum PrimitiveType {

    BOOLEAN("Boolean"),
    CHAR("Char"),
    BYTE("Byte"),
    SHORT("Short"),
    INT("Int"),
    FLOAT("Float"),
    LONG("Long"),
    DOUBLE("Double"),
    ;
    
    private final String typeName;
    private final String arrayTypeName;

    private PrimitiveType(String typeName) {
        this.typeName = typeName;
        this.arrayTypeName = typeName + "Array";
    }

    public String getTypeName() {
        return typeName;
    }

    public String getArrayTypeName() {
        return arrayTypeName;
    }
}
