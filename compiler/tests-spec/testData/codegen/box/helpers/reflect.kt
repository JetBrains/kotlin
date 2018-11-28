// WITH_RUNTIME
// WITH_REFLECT
// FULL_JDK

<!PACKAGE!>

import kotlin.reflect.*
import kotlin.reflect.full.*

fun checkAnnotation(className: String, annotationName: String) =
    Class.forName(className).annotations.find { it.annotationClass.qualifiedName == annotationName } != null

fun checkSuperClass(classRef: KClass<*>, superClassName: String) =
    classRef.superclasses.find { it.qualifiedName == superClassName } != null

private fun getClassTypeParameter(classRef: KClass<*>, typeParameter: String) =
    classRef.typeParameters.find { it.name == typeParameter }

private fun getCallableTypeParameter(callableRef: KCallable<*>, typeParameter: String) =
    callableRef.typeParameters.find { it.name == typeParameter }

private fun getParameter(functionRef: KFunction<*>, parameterName: String) =
    functionRef.parameters.find { it.name == parameterName }

fun checkClassTypeParameter(classRef: KClass<*>, typeParameter: String) = getClassTypeParameter(classRef, typeParameter) != null

fun checkClassTypeParameters(classRef: KClass<*>, typeParameters: List<String>) =
    typeParameters.all { checkClassTypeParameter(classRef, it) }

fun checkCallableTypeParameter(callableRef: KCallable<*>, typeParameter: String) =
    getCallableTypeParameter(callableRef, typeParameter) != null

fun checkCallableTypeParameters(callableRef: KCallable<*>, typeParameters: List<String>) =
    typeParameters.all { checkCallableTypeParameter(callableRef, it) }

fun checkTypeUpperBounds(typeParameter: KTypeParameter, typeParameters: List<String>) =
    typeParameters.all { typeParameterName: String ->
        typeParameter.upperBounds.find { it.toString() == typeParameterName } != null
    }

fun checkClassTypeParametersWithUpperBounds(classRef: KClass<*>, typeParameters: List<Pair<String, List<String>>>) =
    typeParameters.all { (parameterName, parameterUpperBounds) ->
        getClassTypeParameter(classRef, parameterName).let { typeParameter: KTypeParameter? ->
            typeParameter != null && checkTypeUpperBounds(typeParameter, parameterUpperBounds)
        }
    }

fun checkCallableTypeParametersWithUpperBounds(callableRef: KCallable<*>, typeParameters: List<Pair<String, List<String>>>) =
    typeParameters.all { (parameterName, parameterUpperBounds) ->
        getCallableTypeParameter(callableRef, parameterName).let { typeParameter: KTypeParameter? ->
            typeParameter != null && checkTypeUpperBounds(typeParameter, parameterUpperBounds)
        }
    }

fun checkSuperTypeAnnotation(classRef: KClass<*>, superClassName: String, annotationName: String): Boolean {
    val superType = classRef.supertypes.find { it.classifier.toString() == superClassName }

    return superType?.annotations?.find { it.annotationClass.qualifiedName == annotationName } != null ?: false
}

fun checkClassName(ref: KClass<*>, expectedQualifiedName: String) = ref.qualifiedName == expectedQualifiedName

fun checkPackageName(fileClass: String, expectedName: String) =
    Class.forName(fileClass).`package`.name == expectedName

fun checkFileAnnotation(fileClass: String, expectedName: String) =
    Class.forName(fileClass)?.annotations?.find { it.annotationClass.qualifiedName == expectedName } != null ?: false

fun checkFileAnnotations(fileClass: String, expectedNames: List<String>) =
    expectedNames.all { checkFileAnnotation(fileClass, it) }

fun checkProperties(classRef: KClass<*>, properties: List<String>) =
    properties.all { property: String ->
        classRef.members.find { it.name == property } != null
    }

fun checkTypeProperties(classRef: KClass<*>, properties: List<Pair<String, String>>) =
    properties.all { (propertyName, propertyType) ->
        classRef.members.find { it.name == propertyName }?.returnType?.toString() == propertyType
    }

fun checkPropertiesWithAnnotation(classRef: KClass<*>, properties: List<Pair<String, List<String>>>) =
    properties.all { (propertyName, propertyAnnotations) ->
        val foundProperty = classRef.members.find { it.name == propertyName }

        foundProperty.let {
            it != null && propertyAnnotations.all { expectedAnnotationName: String ->
                it.annotations.find { it.annotationClass.qualifiedName == expectedAnnotationName } != null
            }
        }
    }

fun checkPropertyAnnotation(propertyRef: KProperty<*>, expectedQualifiedName: String) =
    propertyRef.annotations.find { it.annotationClass.qualifiedName == expectedQualifiedName } != null

fun checkPropertyType(propertyRef: KProperty<*>, expectedType: String) =
    propertyRef.returnType.toString() == expectedType

fun checkFunctionAnnotation(functionRef: KFunction<*>, expectedQualifiedName: String) =
    functionRef.annotations.find { it.annotationClass.qualifiedName == expectedQualifiedName } != null

fun checkCompanionObjectName(classRef: KClass<*>, expectedQualifiedName: String) =
    classRef.companionObject?.qualifiedName == expectedQualifiedName

fun checkFunctionName(functionRef: KFunction<*>, expectedQualifiedName: String) = functionRef.name == expectedQualifiedName

fun checkSetterParameterName(propertyRef: KMutableProperty<*>, expectedName: String) =
    propertyRef.setter.parameters.find { it.name == expectedName } != null

fun checkParameterType(functionRef: KFunction<*>, parameterName: String, expectedType: String) =
    getParameter(functionRef, parameterName)?.type.toString() == expectedType ?: false

fun checkParameter(functionRef: KFunction<*>, parameterName: String) =
    getParameter(functionRef, parameterName) != null

fun checkParameters(functionRef: KFunction<*>, parameterNames: List<String>) =
    parameterNames.all { checkParameter(functionRef, it) }

fun checkCallableName(property: KCallable<*>, propertyName: String) = property.name == propertyName
