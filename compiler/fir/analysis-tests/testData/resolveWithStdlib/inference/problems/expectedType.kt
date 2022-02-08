import kotlin.reflect.KProperty

abstract class FirProperty {
    abstract val returnTypeRef: FirTypeRef
}

abstract class FirTypeRef

abstract class FirResolvedTypeRef : FirTypeRef() {
    abstract val type: ConeKotlinType
}

abstract class ConeKotlinType

inline fun <reified C : ConeKotlinType> FirTypeRef.coneTypeSafe(): C? {
    return (this as? FirResolvedTypeRef)?.type as? C
}

public fun <L> myLazy(initializer: () -> L): MyLazy<L> = MyLazy(initializer)

public operator fun <V> MyLazy<V>.getValue(thisRef: Any?, property: KProperty<*>): V = value

class MyLazy<out M>(initializer: () -> M) {
    private var _value: Any? = null

    val value: M get() = _value <!UNCHECKED_CAST!>as M<!>
}

class Session(val property: FirProperty) {
    val expectedType: ConeKotlinType? by myLazy { property.returnTypeRef.coneTypeSafe() }
}
