package test;

import java.lang.String;
import java.util.List;

public enum AnnotatedEnumEntry {
    @Anno("a")
    E1,
    @Anno("b")
    @Anno2
    E2,
    E3;

    public static @interface Anno {
        String value();
    }

    public static @interface Anno2 {
    }
}
