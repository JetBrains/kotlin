// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-76284

// FILE: NamedDomainObjectCollection.java
public interface NamedDomainObjectCollection<T> {
  T getByName();
}

// FILE: test.kt
interface KotlinCompilation<T>

val <T> KotlinCompilation<T>.internal get() = 1

fun doSmth(compilations: NamedDomainObjectCollection<out KotlinCompilation<out Any>>) {
    val compilation = compilations.getByName()
    compilation.internal // works

    compilations.getByName().internal
}

/* GENERATED_FIR_TAGS: capturedType, flexibleType, functionDeclaration, getter, integerLiteral, interfaceDeclaration,
javaType, localProperty, nullableType, outProjection, propertyDeclaration, propertyWithExtensionReceiver, typeParameter */
