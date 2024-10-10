// FIR_IDENTICAL

// MODULE: from

// FILE: root.kt

open class Klass {
    fun klassFunction() {}

    open class KlassOwnedKlass {
        fun klassOwnedKlassFunction() {}
    }

    object KlassOwnedObjekt {
        fun klassOwnedObjektFunction() {}
    }

    companion object {
        fun companionObjektFunction() {}
    }
}

object Objekt {
    fun objektFunction() {}

    open class ObjektOwnedKlass {
        fun objektOwnedKlassFunction() {}
    }

    object ObjektOwnedObjekt {
        fun objektOwnedObjektFunction() {}
    }
}

// FILE: JavaKlass.java

public class JavaKlass {
    public void javaKlassFunction() {}
    public static void javaKlassStaticFunction() {}

    public static class NestedJavaKlass {
        public void nestedJavaKlassFunction() {}
        public static void nestedJavaKlassStaticFunction() {}
    }
}

// FILE: pkgAA.kt
package pkg

import Klass.KlassOwnedKlass
import Klass.KlassOwnedObjekt

import Objekt.ObjektOwnedKlass
import Objekt.ObjektOwnedObjekt

import JavaKlass.NestedJavaKlass

import Klass.Companion

fun testAA() {
    val klassOwnedKlass: KlassOwnedKlass? = null
    val klassOwnedObjekt: KlassOwnedObjekt? = null
    val objektOwnedKlass: ObjektOwnedKlass? = null
    val objektOwnedObjekt: ObjektOwnedObjekt? = null
    val nestedJavaKlass: NestedJavaKlass? = null
    val companionObjekt: Companion? = null

    val klassOwnedKlassInstance = KlassOwnedKlass()
    val objektOwnedKlassInstance = ObjektOwnedKlass()
    val nestedJavaKlassInstance = NestedJavaKlass()

    val klassOwnedObjektInstance = KlassOwnedObjekt
    val objektOwnedObjektInstance = ObjektOwnedObjekt
    val companionObjektInstance = Companion
}

// FILE: pkgAB.kt
package pkg

import Klass.KlassOwnedObjekt.klassOwnedObjektFunction

import Objekt.objektFunction
import Objekt.ObjektOwnedObjekt.objektOwnedObjektFunction

import JavaKlass.javaKlassStaticFunction
import JavaKlass.NestedJavaKlass.nestedJavaKlassStaticFunction

import Klass.Companion.companionObjektFunction

fun testAB() {
    klassOwnedObjektFunction()
    objektFunction()
    objektOwnedObjektFunction()
    javaKlassStaticFunction()
    nestedJavaKlassStaticFunction()
    companionObjektFunction()
}

// MODULE: to(from)

// FILE: pkgBA.kt
package pkg

import Klass.KlassOwnedKlass
import Klass.KlassOwnedObjekt

import Objekt.ObjektOwnedKlass
import Objekt.ObjektOwnedObjekt

import JavaKlass.NestedJavaKlass

import Klass.Companion

fun testBA() {
    val klassOwnedKlass: KlassOwnedKlass? = null
    val klassOwnedObjekt: KlassOwnedObjekt? = null
    val objektOwnedKlass: ObjektOwnedKlass? = null
    val objektOwnedObjekt: ObjektOwnedObjekt? = null
    val nestedJavaKlass: NestedJavaKlass? = null
    val companionObjekt: Companion? = null

    val klassOwnedKlassInstance = KlassOwnedKlass()
    val objektOwnedKlassInstance = ObjektOwnedKlass()
    val nestedJavaKlassInstance = NestedJavaKlass()

    val klassOwnedObjektInstance = KlassOwnedObjekt
    val objektOwnedObjektInstance = ObjektOwnedObjekt
    val companionObjektInstance = Companion
}

// FILE: pkgBB.kt
package pkg

import Klass.KlassOwnedObjekt.klassOwnedObjektFunction

import Objekt.objektFunction
import Objekt.ObjektOwnedObjekt.objektOwnedObjektFunction

import JavaKlass.javaKlassStaticFunction
import JavaKlass.NestedJavaKlass.nestedJavaKlassStaticFunction

import Klass.Companion.companionObjektFunction

fun testBB() {
    klassOwnedObjektFunction()
    objektFunction()
    objektOwnedObjektFunction()
    javaKlassStaticFunction()
    nestedJavaKlassStaticFunction()
    companionObjektFunction()
}
