// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann<!>

expect val <!REDECLARATION!>onGetter<!>: String
    @Ann get

expect val <!REDECLARATION!>onGetterImplicit<!>: String
    @Ann get

@get:Ann
expect val <!REDECLARATION!>onGetterWithExplicitTarget<!>: String

@get:Ann
expect val <!REDECLARATION!>explicitTargetMatchesWithoutTarget<!>: String

@get:Ann
expect val <!REDECLARATION!>setOnPropertyWithoutTargetNotMatch<!>: String

expect var <!REDECLARATION!>onSetter<!>: String
    @Ann set

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual val <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>onGetter<!>: String
    get() = ""

actual val <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>onGetterImplicit<!>: String = ""

actual val <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>onGetterWithExplicitTarget<!>: String
    get() = ""

actual val explicitTargetMatchesWithoutTarget: String
    @Ann get() = ""

@Ann
actual val <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>setOnPropertyWithoutTargetNotMatch<!>: String = ""

actual var <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>onSetter<!>: String
    get() = ""
    set(_) {}
