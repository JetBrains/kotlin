// FILE: foo/Super.java
package foo

public abstract class Super<T> {
    protected abstract String getName();
    protected abstract void setName(String s);

    protected abstract String getName2();
    protected abstract void setName2(String s);

    protected abstract void doSomething();
    protected abstract void doSomething2();
}

// FILE: bar/Sub.kt
package bar

abstract class Sub<T>: foo.Super<T>() {
    abstract override fun getName(): String
    abstract override fun setName(s: String)
    abstract override fun doSomething()
}

// FILE: foo/test.kt
package foo

fun test(s: bar.Sub<String>) {
    s.<!INVISIBLE_REFERENCE!>name<!>
    s.<!INVISIBLE_REFERENCE!>name<!> = ""
    s.name2
    s.name2 = ""
    s.<!INVISIBLE_REFERENCE!>doSomething<!>()
    s.doSomething2()
    val s2: Super<String> = s
    s2.name
    s2.name = ""
    s2.name2
    s2.name2 = ""
    s2.doSomething()
    s2.doSomething2()
}
