// FILE: BaseClass.java
public class BaseClass {
    public static int bar = 1;
}

// FILE: Child.java
public class Child extends BaseClass {

}

// FILE: main.kt
package another

import Child.bar

fun usage() {
    <expr>bar</expr>
}
