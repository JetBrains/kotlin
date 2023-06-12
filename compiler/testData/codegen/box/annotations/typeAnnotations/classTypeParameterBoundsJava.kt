// IGNORE_BACKEND: JVM
// TARGET_BACKEND: JVM
// EMIT_JVM_TYPE_ANNOTATIONS
// !LANGUAGE: +ClassTypeParameterAnnotations
// JVM_TARGET: 1.8
// WITH_REFLECT
// FULL_JDK

package foo

import java.lang.reflect.AnnotatedType
import java.lang.reflect.TypeVariable
import java.lang.reflect.AnnotatedParameterizedType
import kotlin.reflect.jvm.javaMethod
import kotlin.test.fail

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn(val name: String)

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class TypeParameterAnn

@Target(AnnotationTarget.TYPE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
annotation class TypeParameterAnnBinary

interface Simple
class SimpleClass
interface Generic<G>
class GenericClass<G>

class SimpleParameter<@TypeParameterAnn @TypeParameterAnnBinary T> {}

class InterfaceBound<@TypeParameterAnn T : @TypeAnn("Simple") Simple> {}

class ClassBound<@TypeParameterAnn T : @TypeAnn("Simple") SimpleClass>

class InterfaceBoundGeneric<T : @TypeAnn("Generic") Generic<@TypeAnn("Simple") Simple>> {}

class ClassBoundGeneric<T : @TypeAnn("GenericClass") GenericClass<@TypeAnn("SimpleClass") SimpleClass>>

class TypeParameterAsBound<Y, @TypeParameterAnn T : @TypeAnn("Y as Bound") Y>

fun box(): String {

    //foo
    checkTypeParameterAnnotation(
        SimpleParameter::class.java.typeParameters.single(),
        "T",
        "@foo.TypeParameterAnn()",
        "foo"
    )

    //interfaceBound
    val interfaceBound = InterfaceBound::class.java
    checkTypeParameterAnnotation(
        interfaceBound.typeParameters.single(),
        "T",
        "@foo.TypeParameterAnn()",
        "interfaceBound type parameter"
    )

    checkTypeAnnotation(
        interfaceBound.typeParameters.single().annotatedBounds.single(),
        "interface foo.Simple",
        "@foo.TypeAnn(name=Simple)",
        "interfaceBound bound"
    )

    //classBound
    val classBound = ClassBound::class.java
    checkTypeParameterAnnotation(
        classBound.typeParameters.single(),
        "T",
        "@foo.TypeParameterAnn()",
        "classBound type parameter"
    )

    checkTypeAnnotation(
        classBound.typeParameters.single().annotatedBounds.single(),
        "class foo.SimpleClass",
        "@foo.TypeAnn(name=Simple)",
        "classBound bound"
    )


    //interfaceBoundGeneric
    val interfaceBoundGeneric = InterfaceBoundGeneric::class.java
    checkTypeAnnotation(
        interfaceBoundGeneric.typeParameters.single().annotatedBounds.single(),
        "foo.Generic<foo.Simple>",
        "@foo.TypeAnn(name=Generic)",
        "interfaceBoundGeneric bound"
    )

    checkTypeAnnotation(
        (interfaceBoundGeneric.typeParameters.single().annotatedBounds.single() as AnnotatedParameterizedType).getAnnotatedActualTypeArguments().single(),
        "interface foo.Simple",
        "@foo.TypeAnn(name=Simple)",
        "interfaceBoundGeneric bound parameter"
    )

    //classBoundGeneric
    val classBoundGeneric = ClassBoundGeneric::class.java
    // Works on JDK 15
//    checkTypeAnnotation(
//        classBoundGeneric.typeParameters.single().annotatedBounds.single(),
//        "foo.GenericClass<foo.SimpleClass>",
//        "@foo.TypeAnn(name=GenericClass)",
//        "classBoundGeneric bound"
//    )

    checkTypeAnnotation(
        (classBoundGeneric.typeParameters.single().annotatedBounds.single() as AnnotatedParameterizedType).getAnnotatedActualTypeArguments().single(),
        "class foo.SimpleClass",
        "@foo.TypeAnn(name=SimpleClass)",
        "classBoundGeneric bound parameter"
    )

    //typeParameterTypeParameterBound
    val typeParameterTypeParameterBound = TypeParameterAsBound::class.java
    checkTypeParameterAnnotation(
        typeParameterTypeParameterBound.typeParameters[1]!!,
        "T",
        "@foo.TypeParameterAnn()",
        "typeParameterTypeParameterBound type parameter"
    )
    // Works on JDK 15
//    checkTypeAnnotation(
//        typeParameterTypeParameterBound.typeParameters[1]!!.annotatedBounds.single(),
//        "Y",
//        "@foo.TypeAnn(name=Y as Bound)",
//        "typeParameterTypeParameterBound bound"
//    )

    return "OK"
}

fun checkTypeParameterAnnotation(
    typeParameter: TypeVariable<*>,
    type: String,
    annotations: String,
    message: String
) {
    if (typeParameter.annotation() != annotations) fail("check $message (1): ${typeParameter.annotation()} != $annotations")

    if (typeParameter.toString() != type) fail("check $message (2): ${typeParameter.toString()} != $type")
}


fun checkTypeAnnotation(
    annotatedType: AnnotatedType,
    type: String,
    annotations: String,
    message: String
) {
    if (annotatedType.annotation() != annotations &&
        //JDK11+
        annotatedType . annotation () != annotations.replace("=", "=\"").replace(")", "\")")
    ) fail("check $message (1): ${annotatedType.annotation()} != $annotations")

    if (annotatedType.type.toString() != type) fail("check $message (2): ${annotatedType.type} != $type")
}


fun AnnotatedType.annotation() = annotations.joinToString()

fun TypeVariable<*>.annotation() = annotations.joinToString()
