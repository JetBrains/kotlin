package one

import kotlin.reflect.KClass

@Repeatable
annotation class AnnoWithKClass(val k: KClass<*>)

@Repeatable
annotation class AnnoWithArrayOfKClass(val a: Array<KClass<*>>)

@AnnoWithArrayOfKClass(<expr>[AnnoWithKClass::class, one.AnnoWithKClass::class]</expr>)
class A