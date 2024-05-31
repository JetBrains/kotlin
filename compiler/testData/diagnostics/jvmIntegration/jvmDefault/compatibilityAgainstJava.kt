// FIR_IDENTICAL
// MODULE: library
// KOTLINC_ARGS: -Xjvm-default=disable
// FILE: test/JavaInterface.java
package test;

public interface JavaInterface<T> {
    default T test(T p) {
        return p;
    }
}

// FILE: test/JavaInterface2.java
package test;

public interface JavaInterface2<T> extends JavaInterface<T> {

}

// MODULE: main(library)
// KOTLINC_ARGS: -Xjvm-default=all-compatibility
// FILE: source.kt
import test.*

interface KotlinInterface<K> : JavaInterface<String> {

}

interface KotlinInterface2 : JavaInterface2<String> {

}

open class KotlinClass : JavaInterface<String> {

}

open class KotlinClass2 : JavaInterface2<String> {

}

open class KotlinClass_2 : KotlinClass() {

}

open class KotlinClass2_2 : KotlinClass2() {

}
