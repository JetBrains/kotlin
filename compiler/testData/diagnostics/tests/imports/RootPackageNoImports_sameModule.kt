// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -DEBUG_INFO_MISSING_UNRESOLVED
// ISSUE: KT-69985
// ISSUE: KT-72170

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

typealias KlassAlias = Klass
typealias KlassOwnedKlassAlias = Klass.KlassOwnedKlass
typealias KlassOwnedObjektAlias = Klass.KlassOwnedObjekt

typealias ObjektAlias = Objekt
typealias ObjektOwnedKlassAlias = Objekt.ObjektOwnedKlass
typealias ObjektOwnedObjektAlias = Objekt.ObjektOwnedObjekt

typealias JavaKlassAlias = JavaKlass
typealias NestedJavaKlassAlias = JavaKlass.NestedJavaKlass

fun topLevelFunction() {}

// FILE: JavaKlass.java

public class JavaKlass {
    public void javaKlassFunction() {}
    public static void javaKlassStaticFunction() {}

    public static class NestedJavaKlass {
        public void nestedJavaKlassFunction() {}
        public static void nestedJavaKlassStaticFunction() {}
    }
}

// FILE: pkgA.kt
package pkg

fun testA() {
    val klass: <!UNRESOLVED_REFERENCE!>Klass<!>? = null
    val klassOwnedKlass: Klass.KlassOwnedKlass? = null
    val klassOwnedObjekt: Klass.KlassOwnedObjekt? = null
    val objekt: <!UNRESOLVED_REFERENCE!>Objekt<!>? = null
    val objektOwnedKlass: Objekt.ObjektOwnedKlass? = null
    val objektOwnedObjekt: Objekt.ObjektOwnedObjekt? = null
    val klassAlias: <!UNRESOLVED_REFERENCE!>KlassAlias<!>? = null
    val klassOwnedKlassAlias: <!UNRESOLVED_REFERENCE!>KlassOwnedKlassAlias<!>? = null
    val klassOwnedObjektAlias: <!UNRESOLVED_REFERENCE!>KlassOwnedObjektAlias<!>? = null
    val objektAlias: <!UNRESOLVED_REFERENCE!>ObjektAlias<!>? = null
    val objektOwnedKlassAlias: <!UNRESOLVED_REFERENCE!>ObjektOwnedKlassAlias<!>? = null
    val objektOwnedObjektAlias: <!UNRESOLVED_REFERENCE!>ObjektOwnedObjektAlias<!>? = null
    val javaKlass: <!UNRESOLVED_REFERENCE!>JavaKlass<!>? = null
    val nestedJavaKlass: JavaKlass.NestedJavaKlass? = null
    val javaKlassAlias: <!UNRESOLVED_REFERENCE!>JavaKlassAlias<!>? = null
    val nestedJavaKlassAlias: <!UNRESOLVED_REFERENCE!>NestedJavaKlassAlias<!>? = null
    val companionObjekt: Klass.Companion? = null

    val klassInstance = <!UNRESOLVED_REFERENCE!>Klass<!>()
    val klassOwnedKlassInstance = Klass.KlassOwnedKlass()
    val objektOwnedKlassInstance = Objekt.ObjektOwnedKlass()
    val klassAliasInstance = <!UNRESOLVED_REFERENCE!>KlassAlias<!>()
    val klassOwnedKlassAliasInstance = <!UNRESOLVED_REFERENCE!>KlassOwnedKlassAlias<!>()
    val objektOwnedKlassAliasInstance = <!UNRESOLVED_REFERENCE!>ObjektOwnedKlassAlias<!>()
    val javaKlassInstance = <!UNRESOLVED_REFERENCE!>JavaKlass<!>()
    val nestedJavaKlassInstance = JavaKlass.NestedJavaKlass()
    val javaKlassAliasInstance = <!UNRESOLVED_REFERENCE!>JavaKlassAlias<!>()
    val nestedJavaKlassAliasInstance = <!UNRESOLVED_REFERENCE!>NestedJavaKlassAlias<!>()

    // ISSUE: KT-72173
    val klassOwnedObjektInstance = Klass.KlassOwnedObjekt
    val objektInstance = <!UNRESOLVED_REFERENCE!>Objekt<!>
    val objektOwnedObjektInstance = Objekt.ObjektOwnedObjekt
    val klassOwnedObjektAliasInstance = <!UNRESOLVED_REFERENCE!>KlassOwnedObjektAlias<!>
    val objektAliasInstance = <!UNRESOLVED_REFERENCE!>ObjektAlias<!>
    val objektOwnedObjektAliasInstance = <!UNRESOLVED_REFERENCE!>ObjektOwnedObjektAlias<!>
    val companionObjektInstance = Klass.Companion

    <!UNRESOLVED_REFERENCE!>topLevelFunction<!>()

    <!UNRESOLVED_REFERENCE!>Klass<!>().klassFunction()
    Klass.KlassOwnedKlass().klassOwnedKlassFunction()
    Objekt.ObjektOwnedKlass().objektOwnedKlassFunction()
    <!UNRESOLVED_REFERENCE!>KlassAlias<!>().klassFunction()
    <!UNRESOLVED_REFERENCE!>KlassOwnedKlassAlias<!>().klassOwnedKlassFunction()
    <!UNRESOLVED_REFERENCE!>ObjektOwnedKlassAlias<!>().objektOwnedKlassFunction()

    // ISSUE: KT-69986
    Klass.KlassOwnedObjekt.klassOwnedObjektFunction()
    Objekt.objektFunction()
    Objekt.ObjektOwnedObjekt.objektOwnedObjektFunction()
    KlassOwnedObjektAlias.klassOwnedObjektFunction()
    ObjektAlias.objektFunction()
    ObjektOwnedObjektAlias.objektOwnedObjektFunction()

    <!UNRESOLVED_REFERENCE!>JavaKlass<!>().javaKlassFunction()
    JavaKlass.NestedJavaKlass().nestedJavaKlassFunction()
    <!UNRESOLVED_REFERENCE!>JavaKlassAlias<!>().javaKlassFunction()
    <!UNRESOLVED_REFERENCE!>NestedJavaKlassAlias<!>().nestedJavaKlassFunction()

    // ISSUE: KT-72171
    JavaKlass.javaKlassStaticFunction()
    JavaKlass.NestedJavaKlass.nestedJavaKlassStaticFunction()
    JavaKlassAlias.javaKlassStaticFunction()
    NestedJavaKlassAlias.nestedJavaKlassStaticFunction()

    // ISSUE: KT-69986
    Klass.companionObjektFunction()
    Klass.Companion.companionObjektFunction()
    KlassAlias.companionObjektFunction()
}
