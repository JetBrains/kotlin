// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
package test

import kotlin.reflect.KClass

annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>ClassArgAnn<!>(val clazz: KClass<*>)

class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>ClassForReference<!> {
    class ClassForReference
}

<!CONFLICTING_OVERLOADS!>@ClassArgAnn(ClassForReference::class)
expect fun getClassExpression()<!>

<!CONFLICTING_OVERLOADS!>@ClassArgAnn(ClassForReference.ClassForReference::class)
expect fun differentClassesWithSameName()<!>

annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>StringArgAnn<!>(val s: String)

<!CONFLICTING_OVERLOADS!>@StringArgAnn("1.9")
expect fun stringConstant()<!>

<!CONFLICTING_OVERLOADS!>@StringArgAnn("1" + ".9")
expect fun stringConcatentation()<!>

object <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Constants<!> {
    const val STR = "1"
}

<!CONFLICTING_OVERLOADS!>@StringArgAnn(Constants.STR)
expect fun constantFromInsideObject()<!>

<!CONFLICTING_OVERLOADS!>@StringArgAnn(Constants.STR + ".9")
expect fun stringConcatentationWithProperty()<!>

enum class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>MyEnum<!> { FOO, BAR }

annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>EnumArgAnn<!>(val e: MyEnum)

<!CONFLICTING_OVERLOADS!>@EnumArgAnn(MyEnum.FOO)
expect fun enumArg()<!>

annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>VarargAnn<!>(vararg val strings: String)

<!CONFLICTING_OVERLOADS!>@VarargAnn("foo", "bar")
expect fun varargInAnnotation()<!>

<!CONFLICTING_OVERLOADS!>@VarargAnn(*["foo", "bar"])
expect fun varargInAnnotationWithArraySpread()<!>

annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>ArrayArgAnn<!>(val strings: Array<String>)

<!CONFLICTING_OVERLOADS!>@ArrayArgAnn(["foo", "bar"])
expect fun arrayInAnnotation()<!>

<!CONFLICTING_OVERLOADS!>@ArrayArgAnn(["foo", "bar"])
expect fun arrayInAnnotationNotMatch()<!>

annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>NestedAnnArg<!>(val text: String, vararg val children: NestedAnnArg)

<!CONFLICTING_OVERLOADS!>@NestedAnnArg(
    text = "root",
    NestedAnnArg("1"),
    NestedAnnArg("2",
                 NestedAnnArg("2.1"),
                 NestedAnnArg("2.2")
    )
)
expect fun complexNestedAnnotations()<!>

<!CONFLICTING_OVERLOADS!>@NestedAnnArg(
    text = "root",
    NestedAnnArg("1"),
    NestedAnnArg("2",
                 NestedAnnArg("2.1"),
                 NestedAnnArg("2.2")
    )
)
expect fun complexNestedAnnotationsNotMatch()<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
package test

@ClassArgAnn(ClassForReference::class)
actual fun getClassExpression() {}

@ClassArgAnn(ClassForReference::class)
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>differentClassesWithSameName<!>() {}

@StringArgAnn("1.9")
actual fun stringConstant() {}

@StringArgAnn("1.9")
actual fun stringConcatentation() {}

@StringArgAnn("1")
actual fun constantFromInsideObject() {}

@StringArgAnn("1.9")
actual fun stringConcatentationWithProperty() {}

@EnumArgAnn(MyEnum.FOO)
actual fun enumArg() {}

@VarargAnn("foo", "bar")
actual fun varargInAnnotation() {}

@VarargAnn("foo", "bar")
actual fun varargInAnnotationWithArraySpread() {}

@ArrayArgAnn(arrayOf("foo", "bar"))
actual fun arrayInAnnotation() {}

@ArrayArgAnn(["foo"])
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>arrayInAnnotationNotMatch<!>() {}

@NestedAnnArg(
    text = "root",
    NestedAnnArg("1"),
    NestedAnnArg("2",
                 NestedAnnArg("2.1"),
                 NestedAnnArg("2.2")
    )
)
actual fun complexNestedAnnotations() {}

@NestedAnnArg(
    text = "root",
    NestedAnnArg("1"),
    NestedAnnArg("2",
                 NestedAnnArg("2.1"),
                 NestedAnnArg("DIFFERENT")
    )
)
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>complexNestedAnnotationsNotMatch<!>() {}
