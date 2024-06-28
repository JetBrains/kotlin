// FILE: main.kt
package test

import dependency.Middle
import dependency.Base

class MyClass : Middle {
    <expr>fun usage() {
        Middle.test()
        Base.test()
    }</expr>
}

// FILE: dependency/Middle.java
package dependency;

public class Middle extends Base {
    public static void test() {}
}

// FILE: dependency/Base.java
package dependency;

public class Base {
    public static void test() {}
}
