// MODULE: m1-common
// FILE: common.kt
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class Ann

expect val onGetter: String
    @Ann get

expect val onGetterImplicit: String
    @Ann get

@get:Ann
expect val onGetterWithExplicitTarget: String

@get:Ann
expect val explicitTargetMatchesWithoutTarget: String

@get:Ann
expect val setOnPropertyWithoutTargetNotMatch: String

expect var onSetter: String
    @Ann set

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual val onGetter: String
    <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>get() = ""<!>

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual val onGetterImplicit: String = ""<!>

actual val onGetterWithExplicitTarget: String
    <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>get() = ""<!>

actual val explicitTargetMatchesWithoutTarget: String
    @Ann get() = ""

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>@Ann
actual val setOnPropertyWithoutTargetNotMatch: String = ""<!>

actual var onSetter: String
    get() = ""
    <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>set(_) {}<!>
