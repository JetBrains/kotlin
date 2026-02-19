// TARGET_BACKEND: JVM
// WITH_STDLIB
// MODULE: lib
// FILE: lib.kt
interface A<T1> {
    fun foo()
}

interface B<T2> : A<T2>

interface C : B<String>

// FILE: FirstHolder.java
public interface FirstHolder<K1> {
    K1 getValue();
}
// FILE: SecondHolder.java
public interface SecondHolder<K2 extends A> { // <--- A is raw
    K2 getValue();
}

// FILE: HolderImpl.java
public interface HolderImpl extends FirstHolder<C> {}

// MODULE: main(lib)
// FILE: main.kt
fun test(s: Any) {
    require(s is SecondHolder<*> && s is HolderImpl)
    /*
     * - Type of s.getValue() is ft<it(A<kotlin/Any?> & C), C?>
     * - foo` is found in scope of it(A<kotlin/Any?> & C)
     *   - there are two candidates (A<Any>.foo and C.foo)
     *   - they have same signatures, so we chose first of them (`A<Any>.foo` in this case)
     * - fir2ir needs to find correct f/o lookup tag for this call
     * - `findClassRepresentation` for `ft<it(A<kotlin/Any?> & C), C?>` in this case should return `null`,
     *     since there is no actual representation for this type
     * - if it won't then fir2ir may decide to generate f/o with `C` lookup tag, based on `A.foo` function,
     *     which is incorrect (there is another f/o in the hierarchy between `C` and `A`
     */
    s.getValue().foo()
}

fun box(): String = "OK"
