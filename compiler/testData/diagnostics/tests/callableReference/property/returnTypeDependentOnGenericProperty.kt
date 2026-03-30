// RUN_PIPELINE_TILL: BACKEND
import kotlin.reflect.KProperty1

fun <T, R> getProperty(x: T, property: KProperty1<T, R>): R =
        property.get(x)

class Person(val name: String)

val name1 = getProperty(Person("John Smith"), Person::name)

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, nullableType, primaryConstructor,
propertyDeclaration, stringLiteral, typeParameter */
