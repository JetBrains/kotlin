// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, operator-call -> paragraph 2 -> sentence 3
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, operator-call -> paragraph 4 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, operator-call -> paragraph 3 -> sentence 1
 * NUMBER: 6
 * DESCRIPTION: Non-extension member callables for delegated properties call
 */

// FILE: LibMyIterator.kt
package libMyIteratorPackage

var isGetCalled = false
var isSetCalled = false



// FILE: Lib1.kt
package libPackage1
import libMyIteratorPackage.*
import testPack.Delegate
import kotlin.reflect.KProperty

operator fun Delegate.getValue(thisRef: Any?, property: KProperty<*>): String {
    return ""
}
operator fun Delegate.setValue(thisRef: Any?, property: KProperty<*>, value: String) {}

// FILE: Lib2.kt
package libPackage2
import libMyIteratorPackage.*
import testPack.Delegate
import kotlin.reflect.KProperty

operator fun Delegate.getValue(thisRef: Any?, property: KProperty<*>): String {
    return ""
}
operator fun Delegate.setValue(thisRef: Any?, property: KProperty<*>, value: String) {}

// FILE: Test.kt
package testPack
import libMyIteratorPackage.*
import libPackage1.getValue
import libPackage1.setValue
import libPackage2.*
import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        isGetCalled = true
        return ""
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        isSetCalled = true
    }
}

class Test {
    var p: String by Delegate()
}

fun box() : String {
    operator fun Delegate.getValue(thisRef: Any?, property: KProperty<*>): String {
        return ""
    }
    operator fun Delegate.setValue(thisRef: Any?, property: KProperty<*>, value: String) {}

    val test = Test()
    assert(!isGetCalled && !isSetCalled)
    test.p = "NEW"
    if (isSetCalled && !isGetCalled) {
        isSetCalled = false
        val x = test.p
        if (!isSetCalled && isGetCalled)
            return "OK"
    }
    return "NOK"
}
