/* open class */
open class OpenClassRemovedTAImpl(x: Int) : Foo()
data class OpenClassRemovedTATypeParameterHolder<T : Foo>(val t: T)
data class OpenClassRemovedTAImplTypeParameterHolder<T : OpenClassRemovedTAImpl>(val t: T)

fun getOpenClassRemovedTA(x: Int): Foo = Foo()
fun setOpenClassRemovedTA(value: Foo?): String = value?.toString() ?: "setOpenClassRemovedTA"
fun getOpenClassRemovedTAImpl(x: Int): Foo = OpenClassRemovedTAImpl(x)
fun setOpenClassRemovedTAImpl(value: OpenClassRemovedTAImpl?): String = value?.toString() ?: "setOpenClassRemovedTAImpl"

fun getOpenClassRemovedTATypeParameterHolder1(x: Int): OpenClassRemovedTATypeParameterHolder<Foo> = OpenClassRemovedTATypeParameterHolder(Foo())
fun getOpenClassRemovedTATypeParameterHolder2(x: Int): OpenClassRemovedTATypeParameterHolder<OpenClassRemovedTAImpl> = OpenClassRemovedTATypeParameterHolder(OpenClassRemovedTAImpl(x))
fun setOpenClassRemovedTATypeParameterHolder1(value: OpenClassRemovedTATypeParameterHolder<Foo>?): String = value?.toString() ?: "setOpenClassRemovedTATypeParameterHolder1"
fun setOpenClassRemovedTATypeParameterHolder2(value: OpenClassRemovedTATypeParameterHolder<OpenClassRemovedTAImpl>?): String = value?.toString() ?: "setOpenClassRemovedTATypeParameterHolder2"

fun getOpenClassRemovedTAImplTypeParameterHolder(x: Int): OpenClassRemovedTAImplTypeParameterHolder<OpenClassRemovedTAImpl> = OpenClassRemovedTAImplTypeParameterHolder(OpenClassRemovedTAImpl(x))
fun setOpenClassRemovedTAImplTypeParameterHolder(value: OpenClassRemovedTAImplTypeParameterHolder<OpenClassRemovedTAImpl>?): String = value?.toString() ?: "setOpenClassRemovedTAImplTypeParameterHolder"

/* interface */
open class InterfaceRemovedTAImpl(x: Int) : Bar
data class InterfaceRemovedTATypeParameterHolder<T : Bar>(val t: T)
data class InterfaceRemovedTAImplTypeParameterHolder<T : InterfaceRemovedTAImpl>(val t: T)

fun getInterfaceRemovedTA(x: Int): Bar = object : Bar {}
fun setInterfaceRemovedTA(value: Bar?): String = value?.toString() ?: "setInterfaceRemovedTA"
fun getInterfaceRemovedTAImpl(x: Int): Bar = InterfaceRemovedTAImpl(x)
fun setInterfaceRemovedTAImpl(value: InterfaceRemovedTAImpl?): String = value?.toString() ?: "setInterfaceRemovedTAImpl"

fun getInterfaceRemovedTATypeParameterHolder1(x: Int): InterfaceRemovedTATypeParameterHolder<Bar> = InterfaceRemovedTATypeParameterHolder(object : Bar {})
fun getInterfaceRemovedTATypeParameterHolder2(x: Int): InterfaceRemovedTATypeParameterHolder<InterfaceRemovedTAImpl> = InterfaceRemovedTATypeParameterHolder(InterfaceRemovedTAImpl(x))
fun setInterfaceRemovedTATypeParameterHolder1(value: InterfaceRemovedTATypeParameterHolder<Bar>?): String = value?.toString() ?: "setInterfaceRemovedTATypeParameterHolder1"
fun setInterfaceRemovedTATypeParameterHolder2(value: InterfaceRemovedTATypeParameterHolder<InterfaceRemovedTAImpl>?): String = value?.toString() ?: "setInterfaceRemovedTATypeParameterHolder2"

fun getInterfaceRemovedTAImplTypeParameterHolder(x: Int): InterfaceRemovedTAImplTypeParameterHolder<InterfaceRemovedTAImpl> = InterfaceRemovedTAImplTypeParameterHolder(InterfaceRemovedTAImpl(x))
fun setInterfaceRemovedTAImplTypeParameterHolder(value: InterfaceRemovedTAImplTypeParameterHolder<InterfaceRemovedTAImpl>?): String = value?.toString() ?: "setInterfaceRemovedTAImplTypeParameterHolder"

/* with type parameter */
open class WithTypeParameterRemovedTAImpl<T>(x: T) : WithTypeParameter<T>(x)
data class WithTypeParameterRemovedTATypeParameterHolder<E, T : WithTypeParameter<E>>(val t: T)
data class WithTypeParameterRemovedTAImplTypeParameterHolder<E, T : WithTypeParameterRemovedTAImpl<E>>(val t: T)

fun <T> getWithTypeParameterRemovedTA(x: T): WithTypeParameter<T> = WithTypeParameter(x)
fun <T> setWithTypeParameterRemovedTA(value: WithTypeParameter<T>?): String = value?.toString() ?: "setWithTypeParameterRemovedTA"
fun <T> getWithTypeParameterRemovedTAImpl(x: T): WithTypeParameter<T> = WithTypeParameterRemovedTAImpl(x)
fun <T> setWithTypeParameterRemovedTAImpl(value: WithTypeParameterRemovedTAImpl<T>?): String = value?.toString() ?: "setWithTypeParameterRemovedTAImpl"

fun getWithTypeParameterRemovedTATypeParameterHolder1(x: Int): WithTypeParameterRemovedTATypeParameterHolder<Int, WithTypeParameter<Int>> = WithTypeParameterRemovedTATypeParameterHolder(WithTypeParameter(x))
fun getWithTypeParameterRemovedTATypeParameterHolder2(x: Int): WithTypeParameterRemovedTATypeParameterHolder<Int, WithTypeParameterRemovedTAImpl<Int>> = WithTypeParameterRemovedTATypeParameterHolder(WithTypeParameterRemovedTAImpl(x))
fun setWithTypeParameterRemovedTATypeParameterHolder1(value: WithTypeParameterRemovedTATypeParameterHolder<Int, WithTypeParameter<Int>>?): String = value?.toString() ?: "setWithTypeParameterRemovedTATypeParameterHolder1"
fun setWithTypeParameterRemovedTATypeParameterHolder2(value: WithTypeParameterRemovedTATypeParameterHolder<Int, WithTypeParameterRemovedTAImpl<Int>>?): String = value?.toString() ?: "setWithTypeParameterRemovedTATypeParameterHolder2"

fun getWithTypeParameterRemovedTAImplTypeParameterHolder(x: Int): WithTypeParameterRemovedTAImplTypeParameterHolder<Int, WithTypeParameterRemovedTAImpl<Int>> = WithTypeParameterRemovedTAImplTypeParameterHolder(WithTypeParameterRemovedTAImpl(x))
fun setWithTypeParameterRemovedTAImplTypeParameterHolder(value: WithTypeParameterRemovedTAImplTypeParameterHolder<Int, WithTypeParameterRemovedTAImpl<Int>>?): String = value?.toString() ?: "setWithTypeParameterRemovedTAImplTypeParameterHolder"

/* nested */
open class NestedRemovedTAImpl(x: Int) : Outer.Nested()
data class NestedRemovedTATypeParameterHolder<T : Outer.Nested>(val t: T)
data class NestedRemovedTAImplTypeParameterHolder<T : NestedRemovedTAImpl>(val t: T)

fun getNestedRemovedTA(x: Int): Outer.Nested = Outer.Nested()
fun setNestedRemovedTA(value: Outer.Nested?): String = value?.toString() ?: "setNestedRemovedTA"
fun getNestedRemovedTAImpl(x: Int): Outer.Nested = NestedRemovedTAImpl(x)
fun setNestedRemovedTAImpl(value: NestedRemovedTAImpl?): String = value?.toString() ?: "setNestedRemovedTAImpl"

fun getNestedRemovedTATypeParameterHolder1(x: Int): NestedRemovedTATypeParameterHolder<Outer.Nested> = NestedRemovedTATypeParameterHolder(Outer.Nested())
fun getNestedRemovedTATypeParameterHolder2(x: Int): NestedRemovedTATypeParameterHolder<NestedRemovedTAImpl> = NestedRemovedTATypeParameterHolder(NestedRemovedTAImpl(x))
fun setNestedRemovedTATypeParameterHolder1(value: NestedRemovedTATypeParameterHolder<Outer.Nested>?): String = value?.toString() ?: "setNestedRemovedTATypeParameterHolder1"
fun setNestedRemovedTATypeParameterHolder2(value: NestedRemovedTATypeParameterHolder<NestedRemovedTAImpl>?): String = value?.toString() ?: "setNestedRemovedTATypeParameterHolder2"

fun getNestedRemovedTAImplTypeParameterHolder(x: Int): NestedRemovedTAImplTypeParameterHolder<NestedRemovedTAImpl> = NestedRemovedTAImplTypeParameterHolder(NestedRemovedTAImpl(x))
fun setNestedRemovedTAImplTypeParameterHolder(value: NestedRemovedTAImplTypeParameterHolder<NestedRemovedTAImpl>?): String = value?.toString() ?: "setNestedRemovedTAImplTypeParameterHolder"
