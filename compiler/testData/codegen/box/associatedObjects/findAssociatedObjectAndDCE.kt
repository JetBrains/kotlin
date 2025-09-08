// DONT_TARGET_EXACT_BACKEND: JVM_IR
// ^ @AssociatedObjectKey is not available in Kotlin/JVM

// WITH_STDLIB

import kotlin.reflect.*

@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class UsedObjectKey1(val kClass: KClass<*>)

@UsedObjectKey1(NotUsedObjectKeeper1.NotUsedObject1::class)
class UsedClass1

class NotUsedObjectKeeper1 {
    companion object NotUsedObject1
}


@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class UsedObjectKey2(val kClass: KClass<*>)

@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class NonUsedObjectKey2(val kClass: KClass<*>)

@NonUsedObjectKey2(NotUsedObjectKeeper2.NotUsedObject2::class)
class UsedClass2

class NotUsedObjectKeeper2 {
    companion object NotUsedObject2
}


@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class UsedObjectKey3(val kClass: KClass<*>)

class UsedClass3

@UsedObjectKey3(NotUsedObjectKeeper3.NotUsedObject3::class)
class NonUsedClass3

class NotUsedObjectKeeper3 {
    companion object NotUsedObject3
}

@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class NotUsedObjectKey4(val kClass: KClass<*>)

@NotUsedObjectKey4(NotUsedObjectKeeper4.NotUsedObject4::class)
class NotUsedClass4

class NotUsedObjectKeeper4 {
    companion object NotUsedObject4
}

@OptIn(ExperimentalAssociatedObjects::class)
fun box(): String {

    if (UsedClass1::class.findAssociatedObject<UsedObjectKey1>() == null) return "fail 1"

    if (UsedClass2::class.findAssociatedObject<UsedObjectKey2>() != null) return "fail 2"

    if (UsedClass3::class.findAssociatedObject<UsedObjectKey3>() != null) return "fail 3"

    return "OK"
}