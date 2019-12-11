// !LANGUAGE: +InlineClasses

import kotlin.reflect.KClass

inline class MyInt(val x: Int)
inline class MyString(val x: String)

annotation class Ann1(val a: MyInt)
annotation class Ann2(val a: Array<MyString>)
annotation class Ann3(vararg val a: MyInt)

annotation class Ann4(val a: KClass<MyInt>)