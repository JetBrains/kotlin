// MODULE: m1-common
// FILE: common.kt

open class Base {
    <!INCOMPATIBLE_MATCHING{JVM}!>open fun foo(param: Int) {}<!>
}

<!INCOMPATIBLE_MATCHING{JVM}!>expect open class Foo1 : Base<!>
expect open class Foo2 : Base
expect open class Foo3 {
    open fun foo(param: Int)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo1 : Base() {
    override fun foo(paramNameChanged: Int) {}
}

actual typealias Foo2 = Foo2Java
actual typealias Foo3 = Foo3Java

// FILE: Foo2Java.java

public class Foo2Java extends Base {
    @Override
    public void foo(int paramNameChanged) {}
}

// FILE: Foo3Java.java
public class Foo3Java {
    public void foo(int paramNameChanged) {}
}
