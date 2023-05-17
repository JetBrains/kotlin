// FILE: main.kt
import Child.staticFunFromBase
import Child.STATIC_CONSTANT_FROM_BASE
import KotlinChild.nonStaticFieldFromBase

fun usage() {
    staticFunFromBase()
    STATIC_CONSTANT_FROM_BASE
    nonStaticFieldFromBase
}

object KotlinChild : Base()

// FILE: JavaClasses.java
class Base {
    static void staticFunFromBase() {}
    static String STATIC_CONSTANT_FROM_BASE = "";

    String nonStaticFieldFromBase = "";
}

class Child extends Base {}