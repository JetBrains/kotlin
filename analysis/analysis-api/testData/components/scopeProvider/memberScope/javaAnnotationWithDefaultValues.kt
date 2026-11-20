// ISSUE: KT-91752
// class: one/JavaAnno
// FILE: main.kt

// FILE: one/JavaEnum.java
package one;

public enum JavaEnum {
    FIRST, SECOND
}

// FILE: one/NestedAnno.java
package one;

public @interface NestedAnno {
    String value();
}

// FILE: one/JavaAnno.java
package one;

import java.util.List;

public @interface JavaAnno {
    int intValue() default 3;

    String stringValue() default "foo";

    Class<?> classValue() default String.class;

    Class<?> genericClassValue() default List.class;

    Class<?> arrayClassValue() default String[].class;

    Class<?> primitiveArrayClassValue() default int[].class;

    Class<?>[] classArrayValue() default {String.class, Integer.class};

    JavaEnum enumValue() default JavaEnum.SECOND;

    JavaEnum[] enumArrayValue() default {JavaEnum.FIRST, JavaEnum.SECOND};

    NestedAnno annotationValue() default @NestedAnno("bar");

    int[] intArrayValue() default {1, 2, 3};

    String withoutDefault();
}
