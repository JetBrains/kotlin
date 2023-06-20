// FILE: main.kt
import dependency.Base
import dependency.Child.staticFunFromBase
import dependency.Child.STATIC_CONSTANT_FROM_BASE
import KotlinChild.nonStaticFieldFromBase

fun usage() {
    staticFunFromBase()
    STATIC_CONSTANT_FROM_BASE
    nonStaticFieldFromBase
}

object KotlinChild : Base()

// FILE: dependency/Base.java
package dependency;

public class Base {
    public static void staticFunFromBase() {}
    public static String STATIC_CONSTANT_FROM_BASE = "";

    public String nonStaticFieldFromBase = "";
}

// FILE: dependency/Child.java
package dependency;

public class Child extends Base {}