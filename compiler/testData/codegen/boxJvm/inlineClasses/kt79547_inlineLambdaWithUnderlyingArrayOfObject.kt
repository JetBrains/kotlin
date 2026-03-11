// TARGET_BACKEND: JVM
// WITH_STDLIB

@JvmInline
public value class KoneMutableArray<Element>(internal val array: Array<Element>)

public inline fun <reified Element> KoneMutableArray(size: UInt, initializer: (UInt) -> Element): KoneMutableArray<Element> =
    KoneMutableArray(Array(size.toInt()) { initializer(it.toUInt()) })

@JvmInline
public value class KoneArraySettableList<Element> @PublishedApi internal constructor(
    internal val data: KoneMutableArray<Any?>,
)

public inline fun <Element> KoneArraySettableList(size: UInt, initializer: (index: UInt) -> Element): KoneArraySettableList<Element> =
    KoneArraySettableList(KoneMutableArray(size, initializer))

fun box(): String {
    val koneList = run {
        val list = listOf(1, 2, 3, 4)
        KoneArraySettableList(list.size.toUInt()) { list[it.toInt()] }
    }
    return "OK"
}
