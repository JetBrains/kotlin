// ISSUE: KT-56506
// FILE: JavaBase.java

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface JavaBase {
    @NotNull String getFoo();

    // Important: no parameter nullability!
    void setFoo(String arg);
}

// FILE: JavaOverride.java
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JavaOverride implements JavaBase {
    @Override
    @NotNull
    public String getFoo() {
        return "";
    }

    // Important: parameter is nullable!
    @Override
    public void setFoo(@Nullable String arg) {}
}

// FILE: NoOverride.java
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NoOverride {
    @NotNull
    public String getFoo() {
        return "";
    }

    // Important: parameter is nullable!
    public void setFoo(@Nullable String arg) {}
}

// FILE: KotlinBase.kt
open class KotlinBase {
    val foo: String
        get() = ""
}

// FILE: KotlinOverrideBase.java
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface KotlinOverrideBase extends KotlinBase {
    @Override
    @NotNull String getFoo();

    void setFoo(String arg);
}

// FILE: KotlinOverride.java
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KotlinOverride implements KotlinOverrideBase {
    @Override
    @NotNull
    public String getFoo() {
        return "";
    }

    // Important: parameter is nullable!
    @Override
    public void setFoo(@Nullable String arg) {}
}

// FILE: main.kt
fun test_1(d: NoOverride, s: String) {
    d.<!VAL_REASSIGNMENT!>foo<!> = s
}

fun test_2(d: JavaBase, s: String) {
    d.foo = s
}

fun test_3(d: JavaOverride, s: String) {
    d.foo = s
}

fun test_4(d: KotlinOverrideBase, s: String) {
    d.<!VAL_REASSIGNMENT!>foo<!> = s
}

fun test_5(d: KotlinOverride, s: String) {
    d.<!VAL_REASSIGNMENT!>foo<!> = s
}
