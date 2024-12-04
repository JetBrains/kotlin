// EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS
// TARGET_BACKEND: JVM_IR

package foo

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn(val name: String)

@Target( AnnotationTarget.TYPE_PARAMETER)
annotation class TypeParameterAnn(val name: String)

@Target(AnnotationTarget.TYPE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
annotation class TypeParameterAnnBinary

@Target(AnnotationTarget.TYPE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class TypeParameterAnnSource

interface SimpleInterface
interface  SimpleInterface2
open class SimpleClass

interface GenericInterface<Z>
open class GenericClass<Z>

class Simple<@TypeParameterAnn("T") @TypeParameterAnnBinary @TypeParameterAnnSource T> {
    fun test(p: T) : T {
        return p
    }
}

class TypeBound<Y, T: @foo.TypeAnn("Y") Y> {
    fun test(p: T) : T {
        return p
    }
}

class InterfaceBound<T: @foo.TypeAnn("Interface") SimpleInterface> {
    fun test(p: T) : T {
        return p
    }
}

class ClassBound<T: @foo.TypeAnn("Class") SimpleClass> {
    fun test(p: T) : T {
        return p
    }
}

class ClassBoundGeneric<T: @foo.TypeAnn("Class") GenericClass<@foo.TypeAnn("SimpleClass") SimpleClass>> {
    fun test(p: T) : T {
        return p
    }
}

class InterfaceBoundGeneric<T: @foo.TypeAnn("Interface") GenericInterface<@foo.TypeAnn("SimpleInterface") SimpleInterface>> {
    fun test(p: T) : T {
        return p
    }
}



class ClassInterfaceBound<T: @foo.TypeAnn("Class") SimpleClass> where T : @foo.TypeAnn("Interface") SimpleInterface, T : @foo.TypeAnn("Interface2") SimpleInterface2 {
    fun test(p: T) : T {
        return p
    }
}

class InterfaceClassBound<T: @foo.TypeAnn("Interface") SimpleInterface > where T : @foo.TypeAnn("Class") SimpleClass, T : @foo.TypeAnn("Interface2") SimpleInterface2 {
    fun test(p: T) : T {
        return p
    }
}

