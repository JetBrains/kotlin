// FIR_IDENTICAL
package test

open class A
interface T

class TypeBoundsAndDelegationSpecifiers<H : Any?, G : Any, C: T>() : A(), T where H : List<String>, G : CharSequence, C: Any?