// FILE: main.kt
import dependency.Base
import dependency.Child
import dependency.Child.staticFunFromBase
import dependency.Child.STATIC_CONSTANT_FROM_BASE
import KotlinChild.nonStaticFieldFromBase

fun usage() {
    Child.staticFunFromBase()
    Child.STATIC_CONSTANT_FROM_BASE
    KotlinChild.nonStaticFieldFromBase
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