// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions

@Target(
    AnnotationTarget.TYPE_PARAMETER,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPE,
)
annotation class Ann

// OK

@Ann
companion fun <@Ann T> String.valid1(@Ann x: @Ann Int): @Ann Int = 1

@Ann
companion var String.valid2: @Ann Int
    @Ann get() = 1
    @Ann set(@Ann v) {}

// NOT OK

companion fun <!COMPANION_EXTENSION_RECEIVER_ANNOTATED!>@Ann<!> String.invalid1() {}

companion fun <!COMPANION_EXTENSION_RECEIVER_ANNOTATED!>@receiver:Ann<!> String.invalid2() {}

companion val <!COMPANION_EXTENSION_RECEIVER_ANNOTATED!>@Ann<!> String.invalid3 = 1

companion val <!COMPANION_EXTENSION_RECEIVER_ANNOTATED!>@receiver:Ann<!> String.invalid4 = 1

companion fun <!COMPANION_EXTENSION_RECEIVER_WITH_TYPE_ARGUMENTS!>List<@Ann String><!>.invalid5() {}

companion val <!COMPANION_EXTENSION_RECEIVER_WITH_TYPE_ARGUMENTS!>List<@Ann String><!>.invalid6 = 1

/* GENERATED_FIR_TAGS: annotationDeclaration, funWithExtensionReceiver, functionDeclaration, getter, integerLiteral,
nullableType, propertyDeclaration, propertyWithExtensionReceiver, setter, typeParameter */
