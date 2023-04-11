// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: NATIVE

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
@BinaryAnnotation("Str" <!EVALUATED("String")!>+ "ing"<!>)
class A @BinaryAnnotation("Str" <!EVALUATED("String")!>+ "ing"<!>) constructor(val i: Int) {
    @BinaryAnnotation("Str" <!EVALUATED("String")!>+ "ing"<!>) constructor() : this(0)
}

// 2. ANNOTATION_CLASS
@BinaryAnnotation("Str" <!EVALUATED("String")!>+ "ing"<!>)
annotation class Anno

// 3. TYPE_PARAMETER
fun <@BinaryAnnotation("Str" <!EVALUATED("String")!>+ "ing"<!>) T, U> fooWithTypeParam(a: T, b: U) {}

// 4. PROPERTY
@BinaryAnnotation("Str" <!EVALUATED("String")!>+ "ing"<!>)
val prop: Int = 0

// 5. FIELD
enum class SomeEnum {
    @BinaryAnnotation("Str" <!EVALUATED("String")!>+ "ing"<!>) A,
    B;
}

@field:BinaryAnnotation("Str" <!EVALUATED("String")!>+ "ing"<!>)
var x: Int = 5

object Delegate {
    operator fun getValue(instance: Any?, property: Any) : String = ""
    operator fun setValue(instance: Any?, property: Any, value: String) {}
}

@delegate:BinaryAnnotation("Str" <!EVALUATED("String")!>+ "ing"<!>)
val p: String by Delegate


// 7. VALUE_PARAMETER
fun @receiver:BinaryAnnotation("Str" <!EVALUATED("String")!>+ "ing"<!>) String.myExtension() {  }
fun foo(@BinaryAnnotation("Str" <!EVALUATED("String")!>+ "ing"<!>) a: Int) {  }

val @receiver:BinaryAnnotation("Str" <!EVALUATED("String")!>+ "ing"<!>) String.a: Int
    get() = 0

class WithConstructorArgumentAnnotation(
    @BinaryAnnotation("Str" <!EVALUATED("String")!>+ "ing"<!>)
    val a: Int
)

@setparam:BinaryAnnotation("Str" <!EVALUATED("String")!>+ "ing"<!>)
var setParamProp: Int = 0
    get() = field + 1
    set(x: Int) { field = x * 2 }

var mutablePropWithAnnotationOnSetterParam = 0
    set(@BinaryAnnotation("Str" <!EVALUATED("String")!>+ "ing"<!>) x: Int) { field = x * 2 }

// 9. FUNCTION
@BinaryAnnotation("Str" <!EVALUATED("String")!>+ "ing"<!>)
fun bar() {}

// 10. PROPERTY_GETTER
// 11. PROPERTY_SETTER
var b: Int
    @BinaryAnnotation("Str" <!EVALUATED("String")!>+ "ing"<!>) get() = 0
    @BinaryAnnotation("Str" <!EVALUATED("String")!>+ "ing"<!>) set(value) {}

// 15. TYPEALIAS
@BinaryAnnotation("Str" <!EVALUATED("String")!>+ "ing"<!>)
typealias C = Int

// MODULE: main
// FILE: main.kt

fun box(): String {
    return "OK"
}
