// one.MyDataClass
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

package one

value class MyValueClass(val str: String, val count: Int)

data class MyDataClass(val value: MyValueClass, val nullable: MyValueClass?)
