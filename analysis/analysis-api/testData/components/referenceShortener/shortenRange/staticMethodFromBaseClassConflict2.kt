// FILE: main.kt
package test

import dependency.Middle
import dependency.Base

class MyClass : Middle {
    <expr>fun usage() {
        Base.test()
    }</expr>
}

// FILE: dependency/Middle.java
package dependency;

public class Middle extends Base {}

// FILE: dependency/Base.java
package dependency;

public class Base {
    public static void test() {}
}
