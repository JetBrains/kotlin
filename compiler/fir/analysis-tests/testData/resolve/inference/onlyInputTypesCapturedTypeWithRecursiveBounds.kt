// FULL_JDK
// WITH_STDLIB
import java.util.EnumMap

interface I

enum class MyEnum : I {
    EnumEntry
}

open class Foo<P>(val data: Map<P, Any?>) where P : Enum<P>, P : I

private fun test(node: Foo<*>) {
    node.data.get(MyEnum.EnumEntry)
    val map = node.data
    map.get(MyEnum.EnumEntry)
}

open class FooEnumMap<P>(val data: EnumMap<P, Any?>) where P : Enum<P>, P : I

private fun test(node: FooEnumMap<*>) {
    node.data.get(MyEnum.EnumEntry)
    val map = node.data
    // TODO inferred type argument is giant multi-level type KT-65704
    // R|<local>/map|.R|kotlin/collections/get|<R|ft<it(kotlin/Enum<out ft<it(kotlin/Enum<out ft<it(kotlin/Enum<*> & I), it(kotlin/Enum<*>? & I?)>> & I), it(kotlin/Enum<*>? & I?)>> & I), it(kotlin/Enum<*>? & I?)>|, R|kotlin/Any!|>(Q|MyEnum|.R|/MyEnum.EnumEntry|)
    map.get(MyEnum.EnumEntry)
}

open class Foo2<P : I>(val data: Map<P, Any?>)

private fun test(node: Foo2<*>) {
    node.data.get(MyEnum.EnumEntry)
    val map = node.data
    map.get(MyEnum.EnumEntry)
}

interface I2

object C: I, I2

open class Foo3<P>(val data: Map<P, Any?>) where P : I, P : I2

fun test3(node: Foo3<*>) {
    node.data.get(C)
}

