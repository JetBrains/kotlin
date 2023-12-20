// !LANGUAGE: -RefineTypeCheckingOnAssignmentsToJavaFields
// WITH_STDLIB

// FILE: Foo.java
public class Foo<T> {
    public T value;
}

// FILE: Foo2.java
import org.jetbrains.annotations.Nullable;

public class Foo2<T> {
    public @Nullable T value;
}

// FILE: Foo3.java
import org.jetbrains.annotations.NotNull;

public class Foo3<T> {
    public @NotNull T value;
}

// FILE: main.kt

// --- from Java --- //

fun takeStarFoo(x: Foo<*>) {
    x.value = <!ASSIGNMENT_TYPE_MISMATCH!>"test"<!>
    x.value <!NONE_APPLICABLE!>+=<!> "test"
}

fun main1() {
    val foo = Foo<Int>()
    foo.value = 1
    takeStarFoo(foo)
    println(foo.value) // CCE: String cannot be cast to Number
}

// --- from Kotlin --- //

public class Bar<T> {
    var value: T = null <!UNCHECKED_CAST!>as T<!>
}

fun takeStarBar(x: Bar<*>) {
    x.value = <!ASSIGNMENT_TYPE_MISMATCH!>"test"<!>
    x.value <!NONE_APPLICABLE!>+=<!> "test"
}

fun main2() {
    val bar = Bar<Int>()
    bar.value = 1
    takeStarBar(bar)
    println(bar.value) // CCE: String cannot be cast to Number
}

// --- from Java (nullable) --- //

fun takeStarFoo2(x: Foo2<*>) {
    x.value = <!ASSIGNMENT_TYPE_MISMATCH!>"test"<!>
    x.value <!NONE_APPLICABLE!>+=<!> "test"
}

fun main3() {
    val foo = Foo2<Int>()
    foo.value = 1
    takeStarFoo2(foo)
    println(foo.value) // CCE: String cannot be cast to Number
}

// --- from Kotlin (nullable) --- //
public class Bar2<T> {
    var value: T? = null
}

fun takeStarBar2(x: Bar2<*>) {
    x.value = <!ASSIGNMENT_TYPE_MISMATCH!>"test"<!>
    x.value <!NONE_APPLICABLE!>+=<!> "test"
}

fun main4() {
    val bar = Bar2<Int>()
    bar.value = 1
    takeStarBar2(bar)
    println(bar.value) // CCE: String cannot be cast to Number
}

// --- from Java (not-null) --- //

fun takeStarFoo3(x: Foo3<*>) {
    x.value = <!ASSIGNMENT_TYPE_MISMATCH!>"test"<!>
    x.value <!NONE_APPLICABLE!>+=<!> "test"
}

fun main5() {
    val foo = Foo3<Int>()
    foo.value = 1
    takeStarFoo3(foo)
    println(foo.value) // CCE: String cannot be cast to Number
}

// --- from Kotlin (field) --- //
class Bar3<T> {
    @JvmField
    var value: T? = null
}

fun takeStarBar3(x: Bar3<*>) {
    x.value = <!ASSIGNMENT_TYPE_MISMATCH!>"test"<!>
    x.value <!NONE_APPLICABLE!>+=<!> "test"
}

fun main6() {
    val bar = Bar3<Int>()
    bar.value = 1
    takeStarBar3(bar)
    println(bar.value) // CCE: String cannot be cast to Number
}
