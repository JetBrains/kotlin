// RUN_PIPELINE_TILL: FRONTEND

interface Foo

@Target(AnnotationTarget.TYPE)
annotation class Ann

class E : <!ANNOTATION_ON_SUPERCLASS_ERROR!>@field:Ann<!> <!ANNOTATION_ON_SUPERCLASS_ERROR!>@get:Ann<!> <!ANNOTATION_ON_SUPERCLASS_ERROR!>@set:Ann<!> <!ANNOTATION_ON_SUPERCLASS_ERROR!>@setparam:Ann<!> Foo

interface G : @Ann Foo

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetField, annotationUseSiteTargetPropertyGetter,
annotationUseSiteTargetPropertySetter, annotationUseSiteTargetSetterParameter, classDeclaration, interfaceDeclaration */
