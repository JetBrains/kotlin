package test;

import java.util.Set;
import java.util.EnumSet;

public enum staticField {
    INSTANCE;
    
    public static int foo = 42;
    
    public static final Set<staticField> INSTANCES = EnumSet.of(INSTANCE);
}
