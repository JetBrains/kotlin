// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JS

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
@BinaryAnnotation(<!EVALUATED("String")!>"Str" + "ing"<!>)
class A @BinaryAnnotation(<!EVALUATED("String")!>"Str" + "ing"<!>) constructor(val i: Int) {
    @BinaryAnnotation(<!EVALUATED("String")!>"Str" + "ing"<!>) constructor() : this(0)
}

// 2. ANNOTATION_CLASS
@BinaryAnnotation(<!EVALUATED("String")!>"Str" + "ing"<!>)
annotation class Anno

// 3. TYPE_PARAMETER
fun <@BinaryAnnotation(<!EVALUATED("String")!>"Str" + "ing"<!>) T, U> fooWithTypeParam(a: T, b: U) {}

// 4. PROPERTY
@BinaryAnnotation(<!EVALUATED("String")!>"Str" + "ing"<!>)
val prop: Int = 0

// 5. FIELD
enum class SomeEnum {
    @BinaryAnnotation(<!EVALUATED("String")!>"Str" + "ing"<!>) A,
    B;
}

@field:BinaryAnnotation(<!EVALUATED("String")!>"Str" + "ing"<!>)
var x: Int = 5

object Delegate {
    operator fun getValue(instance: Any?, property: Any) : String = ""
    operator fun setValue(instance: Any?, property: Any, value: String) {}
}

@delegate:BinaryAnnotation(<!EVALUATED("String")!>"Str" + "ing"<!>)
val p: String by Delegate


// 7. VALUE_PARAMETER
fun @receiver:BinaryAnnotation(<!EVALUATED("String")!>"Str" + "ing"<!>) String.myExtension() {  }
fun foo(@BinaryAnnotation(<!EVALUATED("String")!>"Str" + "ing"<!>) a: Int) {  }

val @receiver:BinaryAnnotation(<!EVALUATED("String")!>"Str" + "ing"<!>) String.a: Int
    get() = 0

class WithConstructorArgumentAnnotation(
    @BinaryAnnotation(<!EVALUATED("String")!>"Str" + "ing"<!>)
    val a: Int
)

@setparam:BinaryAnnotation(<!EVALUATED("String")!>"Str" + "ing"<!>)
var setParamProp: Int = 0
    get() = field + 1
    set(x: Int) { field = x * 2 }

var mutablePropWithAnnotationOnSetterParam = 0
    set(@BinaryAnnotation(<!EVALUATED("String")!>"Str" + "ing"<!>) x: Int) { field = x * 2 }

// 9. FUNCTION
@BinaryAnnotation(<!EVALUATED("String")!>"Str" + "ing"<!>)
fun bar() {}

// 10. PROPERTY_GETTER
// 11. PROPERTY_SETTER
var b: Int
    @BinaryAnnotation(<!EVALUATED("String")!>"Str" + "ing"<!>) get() = 0
    @BinaryAnnotation(<!EVALUATED("String")!>"Str" + "ing"<!>) set(value) {}

// 15. TYPEALIAS
@BinaryAnnotation(<!EVALUATED("String")!>"Str" + "ing"<!>)
typealias C = Int

// MODULE: main
// FILE: main.kt

fun box(): String {
    return "OK"
}
