// LANGUAGE: +RepeatableAnnotations
// TARGET_BACKEND: JVM_IR
// JVM_ABI_K1_K2_DIFF: KT-69075
// WITH_STDLIB
// FULL_JDK
// JVM_TARGET: 1.8
// FILE: box.kt

package test

import test.A
import test.As

@A("class1") @A("class2")
class Z @A("constructor1") @A("constructor2") constructor() {
    @A("nestedClass1") @A("nestedClass2")
    annotation class Nested

    @A("memberFunction1") @A("memberFunction2")
    fun memberFunction() {}

    @A("memberProperty1") @A("memberProperty2")
    var memberProperty: Int
        @A("propertyGetter1") @A("propertyGetter2")
        get() = 0
        @A("propertySetter1") @A("propertySetter2")
        set(value) {}
}

@A("topLevelFunction1") @A("topLevelFunction2")
fun topLevelFunction2(
    @A("parameter1") @A("parameter2")
    parameter: String
) {}

@A("topLevelProperty1") @A("topLevelProperty2")
var String.z: Z
    @A("propertyGetter1") @A("propertyGetter2")
    get() = Z()
    @A("propertySetter1") @A("propertySetter2")
    set(value) {}

@get:A("useSitePropertyGetter1")
@get:A("useSitePropertyGetter2")
@field:A("field1")
@field:A("field2")
val o: String = ""

// FILE: test/A.java

package test;

import java.lang.annotation.*;

@Repeatable(As.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface A {
    String value();
}

// FILE: test/As.java

package test;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface As {
    A[] value();
}
