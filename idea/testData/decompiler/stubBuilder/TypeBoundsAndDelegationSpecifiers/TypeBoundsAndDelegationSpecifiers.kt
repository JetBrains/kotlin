package test

open class A
trait T

class TypeBoundsAndDelegationSpecifiers<H : Any?, G : Any, C: T>() : A(), T where H : List<String>, G : CharSequence, C: Any?