// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +JvmInlineMultiFieldValueClasses
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS

import kotlin.reflect.KProperty

@Repeatable
annotation class Ann

@Ann
@JvmInline
value class A @Ann constructor(
    @Ann
    @param:Ann
    @property:Ann
    @field:Ann
    @get:Ann
    val x: Int,
    @Ann
    @param:Ann
    @property:Ann
    @field:Ann
    @get:Ann
    val y: Int,
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return 0
    }
}

@Ann
@JvmInline
value class B @Ann constructor(
    <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>@Ann<!>
    <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>@param:Ann<!>
    @property:Ann
    <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>@field:Ann<!>
    <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>@get:Ann<!>
    val x: A,
    @Ann
    @param:Ann
    @property:Ann
    @field:Ann
    @get:Ann
    val y: A?,
) {
    <!INAPPLICABLE_JVM_NAME!>@JvmName("otherName")<!>
    fun f() = Unit
}

typealias NullableA = A?

@Ann
class C @Ann constructor(
    <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>@Ann<!>
    <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>@param:Ann<!>
    @property:Ann
    <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>@field:Ann<!>
    <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>@get:Ann<!>
    @set:Ann
    <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>@setparam:Ann<!>
    var x: A,
    @Ann
    @param:Ann
    @property:Ann
    @field:Ann
    @get:Ann
    @set:Ann
    @setparam:Ann
    var y: A?,
    @Ann
    @param:Ann
    @property:Ann
    @field:Ann
    @get:Ann
    @set:Ann
    @setparam:Ann
    var yTa: NullableA,
) {
    @delegate:Ann
    @property:Ann
    <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>@get:Ann<!>
    val z by lazy { A(-100, -200) }
    <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>@delegate:Ann<!>
    @property:Ann
    @get:Ann
    val c by A(-100, -200)
    @delegate:Ann
    @property:Ann
    <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>@get:Ann<!>
    val d by ::z
    
    <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET, INAPPLICABLE_JVM_FIELD!>@JvmField<!>
    val e = x
    
    init {
        if (2 + 2 == 4) {
            @Ann
            val x = 4
            <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>@Ann<!>
            val y = A(1, 2)
        }
        
        
        fun f() {
            if (2 + 2 == 4) {
                @Ann
                val x = 4
                <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>@Ann<!>
                val y = A(1, 2)
            }
        }
    }
}


@Ann
fun <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>@receiver:Ann<!> A.t(<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>@Ann<!> a: A, <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>@Ann<!> b: B, @Ann c: C) {
    if (2 + 2 == 4) {
        @Ann
        val x = 4
        <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>@Ann<!>
        val y = A(1, 2)
    }

    fun f() {
        if (2 + 2 == 4) {
            @Ann
            val x1 = 4
            <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>@Ann<!>
            val y1 = A(1, 2)
        }
    }
}

@Ann
fun @receiver:Ann C.t(<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>@Ann<!> a: A, <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>@Ann<!> b: B, @Ann c: C) = 4

@Ann
var <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>@receiver:Ann<!> A.t
    @Ann
    get() = A(1, 2)
    @Ann
    set(<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>@Ann<!> _) = Unit

@Ann
var @receiver:Ann C.t
    @Ann
    get() = A(1, 2)
    @Ann
    set(<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>@Ann<!> _) = Unit

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetField, annotationUseSiteTargetFieldDelegate,
annotationUseSiteTargetParam, annotationUseSiteTargetProperty, annotationUseSiteTargetPropertyGetter,
annotationUseSiteTargetPropertySetter, annotationUseSiteTargetReceiver, annotationUseSiteTargetSetterParameter,
callableReference, classDeclaration, equalityExpression, funWithExtensionReceiver, functionDeclaration, getter,
ifExpression, init, integerLiteral, lambdaLiteral, localFunction, localProperty, nullableType, operator,
primaryConstructor, propertyDeclaration, propertyDelegate, propertyWithExtensionReceiver, setter, starProjection,
stringLiteral, typeAliasDeclaration, value */
