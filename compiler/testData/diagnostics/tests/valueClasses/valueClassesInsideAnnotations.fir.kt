// !LANGUAGE: +InlineClasses

package kotlin.jvm

import kotlin.reflect.KClass

annotation class JvmInline

@JvmInline
value class MyInt(val x: Int)
@JvmInline
value class MyString(val x: String)

annotation class Ann1(val a: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>MyInt<!>)
annotation class Ann2(val a: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Array<MyString><!>)
annotation class Ann3(vararg val a: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>MyInt<!>)

annotation class Ann4(val a: KClass<MyInt>)