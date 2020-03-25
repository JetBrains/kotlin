// FILE: SomeClass.java
import org.jetbrains.annotations.Nullable;

public class SomeClass {
    @Nullable
    public CharSequence getBar();

    public int getFoo();
}

// FILE: test.kt

class AnotherClass(val bar: CharSequence?, val foo: Int) {
    fun baz(): Any = true
}

fun test1(x: AnotherClass?) {
    val bar = x?.bar ?: return
    x.bar
}

fun test2(x: SomeClass?) {
    val bar = x?.bar ?: return
    x.bar
}

fun test3(x: AnotherClass?) {
    val bar = x?.bar
    if (bar != null) {
        x.bar.length
    }
}

fun test4(x: SomeClass?) {
    val bar = x?.bar
    if (bar != null) {
        x.bar.<!INAPPLICABLE_CANDIDATE!>length<!>
    }
}

fun test5(x: AnotherClass?) {
    val bar = x?.bar as? String ?: return
    x.foo
}

fun test6(x: SomeClass?) {
    val bar = x?.bar as? String ?: return
    x.foo
}

fun test7(x: AnotherClass?) {
    val baz = x?.baz() as? Boolean ?: return
    x.foo
}

fun test8(x: AnotherClass?) {
    val bar = x?.bar ?: return
    x.foo
}

fun test9(x: AnotherClass?) {
    val baz = x?.baz() ?: return
    x.foo
}

