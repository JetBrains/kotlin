// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// TARGET_BACKEND: JVM
// JVM_TARGET: 28
// ENABLE_JVM_PREVIEW
// VALHALLA_VALUE_CLASSES
// WITH_STDLIB
// JDK_KIND: FULL_JDK_21
import java.lang.ref.ReferenceQueue
import java.util.IdentityHashMap
import java.util.WeakHashMap

value class Full(val x: Int, val y: Int)

typealias ByIdentity<T> = IdentityHashMap<Any, T>

typealias FullKeys = IdentityHashMap<Full, Int>

typealias FullQueue = ReferenceQueue<Full>

typealias WeakFullKeys<V> = WeakHashMap<Full, V>

fun test() {
    ByIdentity<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_OBJECT!>Full<!>>()
    FullKeys()
    FullQueue()
    WeakFullKeys<<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_OBJECT!>Int<!>>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, primaryConstructor, propertyDeclaration,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter, value */
