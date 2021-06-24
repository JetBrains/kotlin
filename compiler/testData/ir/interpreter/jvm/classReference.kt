// IGNORE_BACKEND: JVM_IR
package test

import kotlin.reflect.KFunction
import kotlin.collections.*

@CompileTimeCalculation
class A(val a: Int, val b: String) {
    val String.propertyWithExtension: Int
        get() = this.length * a

    fun Int.funWithExtension(other: Int) = this + other
}

const val aSimpleName = A::class.simpleName<!EVALUATED: `A`!>!!<!>
const val aQualifiedName = A::class.qualifiedName<!EVALUATED: `test.A`!>!!<!>
//const val aMembers = A::class.members.joinToString() TODO -> `val test.A.a: kotlin.Int, val test.A.b: kotlin.String, val test.A.(kotlin.String.)propertyWithExtension: kotlin.Int, fun test.A.(kotlin.Int.)funWithExtension(kotlin.Int): kotlin.Int, fun test.A.equals(kotlin.Any?): kotlin.Boolean, fun test.A.hashCode(): kotlin.Int, fun test.A.toString(): kotlin.String`
const val aConstructors = A::class.constructors.<!EVALUATED: `fun <init>(kotlin.Int, kotlin.String): test.A`!>joinToString()<!>
const val aVisibility = A::class.visibility.<!EVALUATED: `PUBLIC`!>toString()<!>
const val aSupertypes = A::class.supertypes.<!EVALUATED: `kotlin.Any`!>joinToString()<!>

@CompileTimeCalculation
interface Base<T>

@CompileTimeCalculation
class B<T, E : T, D : Any>(val prop: T) : Base<T> {
    fun get(): T = prop

    fun getThis(): B<T, out E, in D> = this

    fun <E : Number> withTypeParameter(num: E) = num.toString()
}

//const val bMembers = B::class.members.joinToString() TODO -> `val test.B<T, E, D>.prop: T, fun test.B<T, E, D>.get(): T, fun test.B<T, E, D>.getThis(): test.B<T, out E, in D>, fun test.B<T, E, D>.withTypeParameter(E): kotlin.String, fun test.B<T, E, D>.equals(kotlin.Any?): kotlin.Boolean, fun test.B<T, E, D>.hashCode(): kotlin.Int, fun test.B<T, E, D>.toString(): kotlin.String`
const val bTypeParameters = B::class.typeParameters.<!EVALUATED: `T, E, D`!>joinToString()<!>
const val bSupertypes = B::class.supertypes.<!EVALUATED: `test.Base<T>, kotlin.Any`!>joinToString()<!>
//const val bReturnType1 = B::class.members.toList()[1].returnType.toString() TODO -> `T`
//const val bReturnType2 = B::class.members.toList()[3].returnType.toString() TODO -> `kotlin.String`

const val arguments1 = (B<Number, Double, Int>(1)::getThis as KFunction<*>).returnType.arguments.<!EVALUATED: `T, out E, in D`!>joinToString()<!>
const val arguments2 = (arrayOf(1)::iterator as KFunction<*>).returnType.arguments.<!EVALUATED: `T`!>joinToString()<!>

@CompileTimeCalculation
class C {
    val String.getLength
        get() = this.length
}

//const val cMember = C::class.members.toList()[0].toString() TODO -> `val test.C.(kotlin.String.)getLength: kotlin.Int`
//const val cMemberReturnType = C::class.members.toList()[0].returnType.classifier.toString() TODO -> `class kotlin.Int`