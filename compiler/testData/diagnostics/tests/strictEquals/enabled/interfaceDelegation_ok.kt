// RUN_PIPELINE_TILL: BACKEND
// SCOPE_DUMP: p1.B:equals, p1.D:equals, p1.G:equals, p1.I:equals, p1.J:equals, p2.D:equals, p3.J6:equals, p3.J7:equals, p4.D:equals, p5.D:equals, p5.F:equals

// FILE: f1.kt

package p1

interface IF { // IF
    override fun equals(@EqualityBound(IF::class) other: Any?): Boolean
}

class B(val c: C): IF by c { // B
    override fun equals(@EqualityBound(B::class) other: Any?): Boolean = true
}

class C : IF { // IF
    override fun equals(@EqualityBound(IF::class) other: Any?): Boolean = true
}

class D(val c: C) : IF by c // IF

abstract class E : IF { // IF
    override fun equals(@EqualityBound(IF::class) other: Any?): Boolean = true
}

class F : E() // IF

class G : IF by F() // IF

class H : E() { // IF
    override fun equals(other: Any?): Boolean = true
}

open class I : IF by H() // IF

class J : I() // IF

// FILE: f2.kt

package p2

class D : AC(), IF by C() { // AC
    override fun equals(other: Any?): Boolean = true
}

class C : AC() { // AC
    override fun equals(other: Any?): Boolean = true
}

abstract class AC : IF { // AC
    abstract override fun equals(@EqualityBound(AC::class) other: Any?): Boolean
}

interface IF { // IF
    override fun equals(@EqualityBound(IF::class) other: Any?): Boolean
}

// FILE: f3.kt

package p3

fun <T> select(a: T, b: T): T = a
fun <T> materialize(): T = null!!

interface J0 { // J0
    override fun equals(@EqualityBound(J0::class) other: Any?): Boolean
}
interface J1 // null

interface J2 : J1, J0 { // J2
    override fun equals(@EqualityBound(J2::class) other: Any?): Boolean
}

interface J3 : J1 { // J1
    override fun equals(@EqualityBound(J1::class) other: Any?): Boolean
}

interface J4 : J2, J3 // J2
interface J5 : J3, J2 // J2

class J6 : J4 by materialize() // J2
class J7 : J5 by materialize() // J2

// FILE: f4.kt

package p4

interface A { // A
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean
}

interface B : A // A

class C : B { // A
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean = true
}

class D : B by C() // A

// FILE: f5.kt

package p5

interface IF // null

class C : IF { // IF
    override fun equals(@EqualityBound(IF::class) other: Any?): Boolean = true
}

class D : IF by C() // IF

interface JF { // JF
    override fun equals(@EqualityBound(JF::class) other: Any?): Boolean
}

interface IJF : JF, IF // JF

class F(ijf: IJF) : IF by ijf, JF by ijf // JF

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, inheritanceDelegation,
interfaceDeclaration, nullableType, operator, override, primaryConstructor, propertyDeclaration */
