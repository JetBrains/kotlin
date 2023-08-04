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
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual val <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>onGetter<!>: String
    get() = ""<!>

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual val <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>onGetterImplicit<!>: String = ""<!>

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual val <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>onGetterWithExplicitTarget<!>: String
    get() = ""<!>

actual val explicitTargetMatchesWithoutTarget: String
    @Ann get() = ""

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>@Ann
actual val <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>setOnPropertyWithoutTargetNotMatch<!>: String = ""<!>

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual var <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>onSetter<!>: String
    get() = ""
    set(_) {}<!>
