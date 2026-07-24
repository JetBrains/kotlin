// LANGUAGE: +IntrinsicConstEvaluation

// MODULE: lib
// FILE: lib.kt

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
@BinaryAnnotation("Str".uppercase())
class A @BinaryAnnotation("Str".uppercase()) constructor(val i: Int) {
    @BinaryAnnotation("Str".uppercase()) constructor() : this(0)
}

// 2. ANNOTATION_CLASS
@BinaryAnnotation("Str".uppercase())
annotation class Anno

// 3. TYPE_PARAMETER
fun <@BinaryAnnotation("Str".uppercase()) T, U> fooWithTypeParam(a: T, b: U) {}

// 4. PROPERTY
@BinaryAnnotation("Str".uppercase())
val prop: Int = 0

// 5. FIELD
enum class SomeEnum {
    @BinaryAnnotation("Str".uppercase()) A,
    B;
}

@field:BinaryAnnotation("Str".uppercase())
var x: Int = 5

object Delegate {
    operator fun getValue(instance: Any?, property: Any) : String = ""
    operator fun setValue(instance: Any?, property: Any, value: String) {}
}

@delegate:BinaryAnnotation("Str".uppercase())
val p: String by Delegate


// 7. VALUE_PARAMETER
fun @receiver:BinaryAnnotation("Str".uppercase()) String.myExtension() {  }
fun foo(@BinaryAnnotation("Str".uppercase()) a: Int) {  }

val @receiver:BinaryAnnotation("Str".uppercase()) String.a: Int
    get() = 0

class WithConstructorArgumentAnnotation(
    @BinaryAnnotation("Str".uppercase())
    val a: Int
)

@setparam:BinaryAnnotation("Str".uppercase())
var setParamProp: Int = 0
    get() = field + 1
    set(x: Int) { field = x * 2 }

var mutablePropWithAnnotationOnSetterParam = 0
    set(@BinaryAnnotation("Str".uppercase()) x: Int) { field = x * 2 }

// 9. FUNCTION
@BinaryAnnotation("Str".uppercase())
fun bar() {}

// 10. PROPERTY_GETTER
// 11. PROPERTY_SETTER
var b: Int
    @BinaryAnnotation("Str".uppercase()) get() = 0
    @BinaryAnnotation("Str".uppercase()) set(value) {}

// 15. TYPEALIAS
@BinaryAnnotation("Str".uppercase())
typealias C = Int

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class AnnotationWithVararg(vararg val array: String)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class AnnotationWithArray(val array: Array<String>)

@AnnotationWithVararg("Str".uppercase(), "String2", "String${3}")
class D

@AnnotationWithArray(["Str".uppercase(), "String2", "String${3}"])
class E

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    return "OK"
}
