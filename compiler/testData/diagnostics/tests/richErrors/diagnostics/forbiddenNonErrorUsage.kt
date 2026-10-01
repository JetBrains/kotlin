// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects
// MODULE: m1
class C : <!NON_ERROR_SUPERTYPE!>NonError<!>

fun test() {
    <!NON_ERROR_GET_CLASS_CALL!>NonError<!>::class
}

expect interface Foo

// MODULE: m2()()(m1)
<!ACTUAL_TYPEALIAS_TO_NON_ERROR!>actual typealias Foo = NonError<!>

/* GENERATED_FIR_TAGS: classDeclaration */
