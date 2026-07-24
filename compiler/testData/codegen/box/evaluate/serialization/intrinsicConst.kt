// LANGUAGE: +IntrinsicConstEvaluation

// MODULE: lib
// FILE: lib.kt

enum class EnumClass {
    OK, VALUE, anotherValue, WITH_UNDERSCORE
}

// Commented targets are not serializable
@Target(
/*1*/   AnnotationTarget.CLASS,
/*2*/   AnnotationTarget.ANNOTATION_CLASS,
/*3*/   AnnotationTarget.TYPE_PARAMETER,
/*4*/   AnnotationTarget.PROPERTY,
/*5*/   AnnotationTarget.FIELD,
/*6*/   // AnnotationTarget.LOCAL_VARIABLE,
/*7*/   AnnotationTarget.VALUE_PARAMETER,
/*8*/   AnnotationTarget.CONSTRUCTOR,
/*9*/   AnnotationTarget.FUNCTION,
/*10*/  AnnotationTarget.PROPERTY_GETTER,
/*11*/  AnnotationTarget.PROPERTY_SETTER,
/*12*/  AnnotationTarget.TYPE,
/*13*/  // AnnotationTarget.EXPRESSION, // can be applied only to source annotations
/*14*/  // AnnotationTarget.FILE,
/*15*/  AnnotationTarget.TYPEALIAS
)
@Retention(AnnotationRetention.BINARY)
annotation class BinaryAnnotation(val str: String)

// 1. CLASS
// 8. CONSTRUCTOR
@BinaryAnnotation(EnumClass.OK.name)
class A @BinaryAnnotation(EnumClass.OK.name) constructor(val i: Int) {
    @BinaryAnnotation(EnumClass.OK.name) constructor() : this(0)
}

// 2. ANNOTATION_CLASS
@BinaryAnnotation(EnumClass.OK.name)
annotation class Anno

// 3. TYPE_PARAMETER
fun <@BinaryAnnotation(EnumClass.OK.name) T, U> fooWithTypeParam(a: T, b: U) {}

// 4. PROPERTY
@BinaryAnnotation(EnumClass.OK.name)
val prop: Int = 0

// 5. FIELD
enum class SomeEnum {
    @BinaryAnnotation(EnumClass.OK.name) A,
    B;
}

@field:BinaryAnnotation(EnumClass.OK.name)
var x: Int = 5

object Delegate {
    operator fun getValue(instance: Any?, property: Any) : String = ""
    operator fun setValue(instance: Any?, property: Any, value: String) {}
}

@delegate:BinaryAnnotation(EnumClass.OK.name)
val p: String by Delegate


// 7. VALUE_PARAMETER
fun @receiver:BinaryAnnotation(EnumClass.OK.name) String.myExtension() {  }
fun foo(@BinaryAnnotation(EnumClass.OK.name) a: Int) {  }

val @receiver:BinaryAnnotation(EnumClass.OK.name) String.a: Int
    get() = 0

class WithConstructorArgumentAnnotation(
    @BinaryAnnotation(EnumClass.OK.name)
    val a: Int
)

@setparam:BinaryAnnotation(EnumClass.OK.name)
var setParamProp: Int = 0
    get() = field + 1
    set(x: Int) { field = x * 2 }

var mutablePropWithAnnotationOnSetterParam = 0
    set(@BinaryAnnotation(EnumClass.OK.name) x: Int) { field = x * 2 }

// 9. FUNCTION
@BinaryAnnotation(EnumClass.OK.name)
fun bar() {}

// 10. PROPERTY_GETTER
// 11. PROPERTY_SETTER
var b: Int
    @BinaryAnnotation(EnumClass.OK.name) get() = 0
    @BinaryAnnotation(EnumClass.OK.name) set(value) {}

// 15. TYPEALIAS
@BinaryAnnotation(EnumClass.OK.name)
typealias C = Int

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class AnnotationWithVararg(vararg val array: String)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class AnnotationWithArray(val array: Array<String>)

@AnnotationWithVararg(EnumClass.OK.name, "String2", "String${3}")
class D

@AnnotationWithArray([EnumClass.OK.name, "String2", "String${3}"])
class E

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    return "OK"
}
