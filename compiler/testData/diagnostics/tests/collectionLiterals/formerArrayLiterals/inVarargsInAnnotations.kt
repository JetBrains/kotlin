// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class Foo
annotation class AnnoFoo

@Repeatable
annotation class IntAnno(vararg val args: Int = [])
@Repeatable
annotation class StringAnno(vararg val args: String = [<!UNRESOLVED_REFERENCE!>[]<!>])
@Repeatable
annotation class FooAnno(vararg val args: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Foo<!>)
@Repeatable
annotation class AnnoFooAnno(vararg val args: AnnoFoo = [AnnoFoo()])
@Repeatable
annotation class NestedAnno(vararg val args: NestedAnno = [NestedAnno(NestedAnno(), NestedAnno(*[]))])
@Repeatable
annotation class ArrayStringAnno(vararg val args: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Array<String><!> = [[<!UNRESOLVED_REFERENCE!>[]<!>]])

@IntAnno
@IntAnno(1, 2, 3)
@IntAnno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>[1, 2, 3]<!>)
@IntAnno(*[1, 2, 3])
@IntAnno(args=[1, 2, 3])
@IntAnno(*[])
@IntAnno(args=[])
@IntAnno(*[<!ARGUMENT_TYPE_MISMATCH!>"1"<!>, <!ARGUMENT_TYPE_MISMATCH!>"2"<!>, <!ARGUMENT_TYPE_MISMATCH!>"3"<!>])
@IntAnno(args=[<!ARGUMENT_TYPE_MISMATCH!>"1"<!>, <!ARGUMENT_TYPE_MISMATCH!>"2"<!>, <!ARGUMENT_TYPE_MISMATCH!>"3"<!>])
@IntAnno(*<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>[]<!>]<!>)
@IntAnno(args=<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>[]<!>]<!>)
@IntAnno(args=<!REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_ANNOTATION!>*<!>[1, 2, 3])
fun intTarget() = Unit

@StringAnno("1", "2", "3")
@StringAnno(*["1", "2", "3"])
@StringAnno(args=["1", <!ARGUMENT_TYPE_MISMATCH!>2<!>, "3"])
@StringAnno(*<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>["1", <!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>["2"]<!>, "3"]<!>)
@StringAnno()
@StringAnno(*[])
@StringAnno(*<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>[]<!>]<!>)
@StringAnno(*<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>[<!UNRESOLVED_REFERENCE!>[]<!>]<!>]<!>)
fun stringTarget() = Unit

@FooAnno
@FooAnno()
@FooAnno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>[]<!>)
@FooAnno(*[])
@FooAnno(*<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>Foo()<!>]<!>)
fun fooTarget() = Unit

val annoFoo = AnnoFoo()

@AnnoFooAnno()
@AnnoFooAnno(AnnoFoo())
@AnnoFooAnno(*[AnnoFoo()])
@AnnoFooAnno(*<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>annoFoo<!>]<!>)
fun annoFooTarget() = Unit

@NestedAnno(NestedAnno(*[NestedAnno(NestedAnno(*[NestedAnno(args=[NestedAnno()])]), NestedAnno(*[NestedAnno(NestedAnno(), NestedAnno(args=[NestedAnno(NestedAnno(), NestedAnno())]))]))]), NestedAnno())
@NestedAnno(args=<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>NestedAnno(args=[NestedAnno(*[NestedAnno(*[NestedAnno(args=[NestedAnno(<!UNRESOLVED_REFERENCE!>[NestedAnno(args=[NestedAnno(*[NestedAnno(*[NestedAnno()])])])]<!>)])])])])<!>]<!>)
@NestedAnno(*arrayOf(NestedAnno(args=[NestedAnno(args=[NestedAnno(*arrayOf(NestedAnno()))])])))
@NestedAnno(*<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>[NestedAnno(args=[NestedAnno()])]<!>]<!>)
@NestedAnno(args=[NestedAnno(*arrayOf(*[NestedAnno(NestedAnno(), NestedAnno())]))])
fun nestedTarget() = Unit

@ArrayStringAnno
@ArrayStringAnno([<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>, <!ARGUMENT_TYPE_MISMATCH!>3<!>])
@ArrayStringAnno(["1", "2", "3"], [])
@ArrayStringAnno([], [])
@ArrayStringAnno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>[]<!>]<!>)
@ArrayStringAnno(*[])
@ArrayStringAnno(*[[]])
@ArrayStringAnno(*[["1", "2", "3"]])
@ArrayStringAnno(*[[<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>, <!ARGUMENT_TYPE_MISMATCH!>3<!>]])
@ArrayStringAnno(*<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>[]<!>]<!>]<!>)
@ArrayStringAnno(*[])
fun arrayStringTarget() = Unit

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, collectionLiteral, functionDeclaration, integerLiteral,
outProjection, primaryConstructor, propertyDeclaration, stringLiteral, vararg */
