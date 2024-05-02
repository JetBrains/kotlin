// FILE: main.kt
package test

import other.ForConstructor
import other.ForRegularMethod
import other.ForStaticMethod

import dependency.JavaClass

fun usage() {
    val jc = JavaClass<ForConstructor>()

    jc.genericMethod<ForRegularMethod>()

    JavaClass.genericStaticMethod<ForStaticMethod>()
}

// FILE: dependency/JavaClass.java
package dependency;

public class JavaClass<TC> {

    public <TM> void genericMethod() {}

    public static <TS> void genericStaticMethod() {}

}

// FILE: dependency/Bar.kt
package other

class ForConstructor
class ForRegularMethod
class ForStaticMethod
