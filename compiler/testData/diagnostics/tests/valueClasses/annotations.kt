// !LANGUAGE: +ValueClasses
// WITH_STDLIB
// SKIP_TXT
// WORKS_WHEN_VALUE_CLASS
// FIR_IDENTICAL

import kotlin.reflect.KProperty

@Repeatable
annotation class Ann

@[Ann Ann]
@JvmInline
value class A @Ann constructor(
    @[Ann Ann]
    @param:[Ann Ann]
    @property:[Ann Ann]
    @field:[Ann Ann]
    @get:[Ann Ann]
    val x: Int,
    @[Ann Ann]
    @param:[Ann Ann]
    @property:[Ann Ann]
    @field:[Ann Ann]
    @get:[Ann Ann]
    val y: Int,
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return 0
    }
}

@[Ann Ann]
@JvmInline
value class B @Ann constructor(
    @[<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!> <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!>]
    @param:[<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!> <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!>]
    @property:[Ann Ann]
    @field:[<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!> <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!>]
    @get:[<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!> <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!>]
    val x: A,
    @[Ann Ann]
    @param:[Ann Ann]
    @property:[Ann Ann]
    @field:[Ann Ann]
    @get:[Ann Ann]
    val y: A?,
) {
    <!INAPPLICABLE_JVM_NAME!>@JvmName("otherName")<!>
    fun f() = Unit
}

@[Ann Ann]
class C @Ann constructor(
    @[<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!> <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!>]
    @param:[<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!> <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!>]
    @property:[Ann Ann]
    @field:[<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!> <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!>]
    @get:[<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!> <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!>]
    @set:[Ann Ann]
    @setparam:[<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!> <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!>]
    var x: A,
    @[Ann Ann]
    @param:[Ann Ann]
    @property:[Ann Ann]
    @field:[Ann Ann]
    @get:[Ann Ann]
    @set:[Ann Ann]
    @setparam:[Ann Ann]
    var y: A?,
) {
    @delegate:[Ann Ann]
    @property:[Ann Ann]
    @get:[<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!> <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!>]
    val z by lazy { A(-100, -200) }
    @delegate:[<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!> <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!>]
    @property:[Ann Ann]
    @get:[Ann Ann]
    val c by A(-100, -200)
    @delegate:[Ann Ann]
    @property:[Ann Ann]
    @get:[<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!> <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!>]
    val d by ::z
    
    <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET, INAPPLICABLE_JVM_FIELD!>@JvmField<!>
    val e = x
    
    init {
        if (2 + 2 == 4) {
            @[Ann Ann]
            val x = 4
            @[<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!> <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!>]
            val y = A(1, 2)
        }
        
        
        fun f() {
            if (2 + 2 == 4) {
                @[Ann Ann]
                val x = 4
                @[<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!> <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!>]
                val y = A(1, 2)
            }
        }
    }
}


@[Ann Ann]
fun @receiver:[<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!> <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!>] A.t(@[<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!> <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!>] a: A, @[<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!> <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!>] b: B, @[Ann Ann] c: C) {
    if (2 + 2 == 4) {
        @[Ann Ann]
        val x = 4
        @[<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!> <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!>]
        val y = A(1, 2)
    }

    fun f() {
        if (2 + 2 == 4) {
            @[Ann Ann]
            val x1 = 4
            @[<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!> <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!>]
            val y1 = A(1, 2)
        }
    }
}

@[Ann Ann]
fun @receiver:[Ann Ann] C.t(@[<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!> <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!>] a: A, @[<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!> <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!>] b: B, @[Ann Ann] c: C) = 4

@[Ann Ann]
var @receiver:[<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!> <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!>] A.t
    @[Ann Ann]
    get() = A(1, 2)
    @[Ann Ann]
    set(@[<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!> <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!>] _) = Unit

@[Ann Ann]
var @receiver:[Ann Ann] C.t
    @[Ann Ann]
    get() = A(1, 2)
    @[Ann Ann]
    set(@[<!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!> <!ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET!>Ann<!>] _) = Unit
