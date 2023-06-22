// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
package test

import kotlin.reflect.KClass

annotation class ClassArgAnn(val clazz: KClass<*>)

class ClassForReference {
    class ClassForReference
}

@ClassArgAnn(ClassForReference::class)
expect fun getClassExpression()

@ClassArgAnn(ClassForReference.ClassForReference::class)
expect fun differentClassesWithSameName()

annotation class StringArgAnn(val s: String)

@StringArgAnn("1.9")
expect fun stringConstant()

@StringArgAnn("1" + ".9")
expect fun stringConcatentation()

object Constants {
    const val STR = "1"
}

@StringArgAnn(Constants.STR)
expect fun constantFromInsideObject()

@StringArgAnn(Constants.STR + ".9")
expect fun stringConcatentationWithProperty()

enum class MyEnum { FOO, BAR }

annotation class EnumArgAnn(val e: MyEnum)

@EnumArgAnn(MyEnum.FOO)
expect fun enumArg()

annotation class VarargAnn(vararg val strings: String)

@VarargAnn("foo", "bar")
expect fun varargInAnnotation()

@VarargAnn(*["foo", "bar"])
expect fun varargInAnnotationWithArraySpread()

annotation class ArrayArgAnn(val strings: Array<String>)

@ArrayArgAnn(["foo", "bar"])
expect fun arrayInAnnotation()

@ArrayArgAnn(["foo", "bar"])
expect fun arrayInAnnotationNotMatch()

annotation class NestedAnnArg(val text: String, vararg val children: NestedAnnArg)

@NestedAnnArg(
    text = "root",
    NestedAnnArg("1"),
    NestedAnnArg("2",
                 NestedAnnArg("2.1"),
                 NestedAnnArg("2.2")
    )
)
expect fun complexNestedAnnotations()

@NestedAnnArg(
    text = "root",
    NestedAnnArg("1"),
    NestedAnnArg("2",
                 NestedAnnArg("2.1"),
                 NestedAnnArg("2.2")
    )
)
expect fun complexNestedAnnotationsNotMatch()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
package test

@ClassArgAnn(ClassForReference::class)
actual fun getClassExpression() {}

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>@ClassArgAnn(ClassForReference::class)
actual fun differentClassesWithSameName() {}<!>

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

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>@ArrayArgAnn(["foo"])
actual fun arrayInAnnotationNotMatch() {}<!>

@NestedAnnArg(
    text = "root",
    NestedAnnArg("1"),
    NestedAnnArg("2",
                 NestedAnnArg("2.1"),
                 NestedAnnArg("2.2")
    )
)
actual fun complexNestedAnnotations() {}

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>@NestedAnnArg(
    text = "root",
    NestedAnnArg("1"),
    NestedAnnArg("2",
                 NestedAnnArg("2.1"),
                 NestedAnnArg("DIFFERENT")
    )
)
actual fun complexNestedAnnotationsNotMatch() {}<!>
