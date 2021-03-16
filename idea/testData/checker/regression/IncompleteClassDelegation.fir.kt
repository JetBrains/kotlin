package c

class C<T>: <error descr="[SUPERTYPE_NOT_A_CLASS_OR_INTERFACE] Supertype is not a class or interface">T</error> by {
}

class D<T>: <error descr="[SUPERTYPE_NOT_A_CLASS_OR_INTERFACE] Supertype is not a class or interface">T</error> by<EOLError descr="Expecting an expression"></EOLError>

class G<T> : <error descr="[SUPERTYPE_NOT_A_CLASS_OR_INTERFACE] Supertype is not a class or interface">T</error> by {

    val c = 3
}

interface I

class A<T : I>(a: T) : <error descr="[SUPERTYPE_NOT_A_CLASS_OR_INTERFACE] Supertype is not a class or interface">T</error> by a
