// RUN_PIPELINE_TILL: BACKEND

// FILE: regular.kt
package n

interface I1 {
    override fun equals(@EqualityBound(I1::class) other: Any?): Boolean
}

interface I2 {
    override fun equals(@EqualityBound(I2::class) other: Any?): Boolean
}

interface I3 : I1, I2 {
    override fun equals(other: Any?): Boolean
}

interface I4 {
    override fun equals(@EqualityBound(I4::class) other: Any?): Boolean
}

interface I5 : I3, I4 {
    override fun equals(other: Any?): Boolean
}

interface I6 : I4, I3 {
    override fun equals(other: Any?): Boolean
}

interface I7 : I5, I6 {
    override fun equals(other: Any?): Boolean
}

interface I10 : I1 {
    override fun equals(@EqualityBound(I10::class) other: Any?): Boolean
}

interface I11 : I10, I1 {
    override fun equals(other: Any?): Boolean
}

interface I12 : I1, I10 {
    override fun equals(other: Any?): Boolean
}

// FILE: generic.kt
package g

class Inv<X>

interface I1<T> {
    override fun equals(@EqualityBound(I1::class) other: Any?): Boolean
}

interface I2<X> : I1<Inv<X>> {
    override fun equals(@EqualityBound(I2::class) other: Any?): Boolean
}

interface I3 : I1<Inv<String>>, I2<String> {
    override fun equals(other: Any?): Boolean
}

interface I4 : I2<String>, I1<Inv<String>> {
    override fun equals(other: Any?): Boolean
}

interface I5<out T> {
    override fun equals(@EqualityBound(I5::class) other: Any?): Boolean
}

interface I6<T> : I5<T> {
    override fun equals(@EqualityBound(I6::class) other: Any?): Boolean
}

interface I7<T> : I5<T>

interface I8 : I5<String>, I7<String>, I6<String>  {
    override fun equals(other: Any?): Boolean
}

interface D1<T> {
    override fun equals(@EqualityBound(D1::class) other: Any?): Boolean
}

interface D2 : D1<String> {
    override fun equals(other: Any?): Boolean
}

interface D3 : D1<String> {
    override fun equals(other: Any?): Boolean
}

interface D4 : D2, D3 {
    override fun equals(other: Any?): Boolean
}

// FILE: interfaceAndClass.kt

package ic

interface I {
    override fun equals(@EqualityBound(I::class) other: Any?): Boolean
}

open class C {
    override fun equals(@EqualityBound(C::class) other: Any?): Boolean = true
}

class D : C(), I {
    override fun equals(other: Any?): Boolean = true
}

class E : I, C() {
    override fun equals(other: Any?): Boolean = true
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, interfaceDeclaration, nullableType,
operator, out, outProjection, override, typeParameter */
