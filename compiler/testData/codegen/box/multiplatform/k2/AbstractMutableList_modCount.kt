// LANGUAGE: +MultiPlatformProjects
// ALLOW_KOTLIN_PACKAGE
// PREFER_IN_TEST_OVER_STDLIB
// TARGET_BACKEND: JVM_IR
// ISSUE: KT-66436
// DUMP_IR

// MODULE: common
// FILE: common.kt
package kotlin.collections

import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty0

public expect abstract class AbstractMutableList() {
    protected var modCount: Int
}

public open class AbstractMyMutableList: AbstractMutableList() {
    fun getModCountAtAbstractMyMutableList(): Int = modCount
    fun incrementModCountAtAbstractMyMutableList() { modCount++ }
    fun getModCountViaReferenceAtAbstractMyMutableList(): Int = getPropertyValue(::modCount)
    fun setModCountViaReferenceAtAbstractMyMutableList(newModCount: Int) { setPropertyValue(::modCount, newModCount) }
}

public class MyMutableList: AbstractMyMutableList() {
    fun getModCountAtMyMutableList(): Int = modCount
    fun incrementModCountAtMyMutableList() { modCount++ }
    fun getModCountViaReferenceAtMyMutableList(): Int = getPropertyValue(::modCount)
    fun setModCountViaReferenceAtMyMutableList(newModCount: Int) { setPropertyValue(::modCount, newModCount) }
}

private fun <T> getPropertyValue(property: KProperty0<T>): T {
    return property.get()
}

private fun <T> setPropertyValue(property: KMutableProperty0<T>, value: T) {
    property.set(value)
}

// MODULE: jvm()()(common)
// FILE: bar/JavaAbstractMutableList.java
package bar; // Java class is in the different package.

public abstract class JavaAbstractMutableList {
    protected transient int modCount = 0;

    public int getModCountAtJava() {
        return modCount;
    }

    public void incrementModCountAtJava() {
        modCount++;
    }
}

// FILE: jvm.kt
package kotlin.collections

public actual abstract class AbstractMutableList actual constructor(): bar.JavaAbstractMutableList()

// FILE: box.kt

fun box(): String {
    val list = MyMutableList()
    list.assertModCount(0)

    list.incrementModCountAtJava()
    list.assertModCount(1)

    list.incrementModCountAtAbstractMyMutableList()
    list.assertModCount(2)

    list.incrementModCountAtMyMutableList()
    list.assertModCount(3)

    list.setModCountViaReferenceAtAbstractMyMutableList(100)
    list.assertModCount(100)

    list.setModCountViaReferenceAtMyMutableList(200)
    list.assertModCount(200)

    return "OK"
}

private fun MyMutableList.assertModCount(expected: Int) {
    val modCountAtJava = (this as AbstractMutableList).modCountAtJava
    val modCountAtAbstractMyMutableList = getModCountAtAbstractMyMutableList()
    val modCountAtMyMutableList = getModCountAtMyMutableList()
    val modCountViaReferenceAtAbstractMyMutableList = getModCountViaReferenceAtAbstractMyMutableList()
    val modCountViaReferenceAtMyMutableList = getModCountViaReferenceAtMyMutableList()

    if (expected != modCountAtJava ||
        expected != modCountAtAbstractMyMutableList ||
        expected != modCountAtMyMutableList ||
        expected != modCountViaReferenceAtAbstractMyMutableList ||
        expected != modCountViaReferenceAtMyMutableList
    ) {
        throw AssertionError(
            "expected: $expected\n" +
                    "modCountAtJava: $modCountAtJava\n" +
                    "modCountAtAbstractMyMutableList: $modCountAtAbstractMyMutableList\n" +
                    "modCountAtMyMutableList: $modCountAtMyMutableList\n" +
                    "modCountViaReferenceAtAbstractMyMutableList: $modCountViaReferenceAtAbstractMyMutableList\n" +
                    "modCountViaReferenceAtMyMutableList: $modCountViaReferenceAtMyMutableList"
        )
    }
}
