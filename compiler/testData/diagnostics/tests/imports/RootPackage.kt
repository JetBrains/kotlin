// FIR_IDENTICAL

// MODULE: from

// FILE: from.kt

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

// FILE: toA.kt

fun testA() {
    val klass: Klass? = null
    val klassOwnedKlass: Klass.KlassOwnedKlass? = null
    val klassOwnedObjekt: Klass.KlassOwnedObjekt? = null
    val objekt: Objekt? = null
    val objektOwnedKlass: Objekt.ObjektOwnedKlass? = null
    val objektOwnedObjekt: Objekt.ObjektOwnedObjekt? = null
    val klassAlias: KlassAlias? = null
    val klassOwnedKlassAlias: KlassOwnedKlassAlias? = null
    val klassOwnedObjektAlias: KlassOwnedObjektAlias? = null
    val objektAlias: ObjektAlias? = null
    val objektOwnedKlassAlias: ObjektOwnedKlassAlias? = null
    val objektOwnedObjektAlias: ObjektOwnedObjektAlias? = null
    val javaKlass: JavaKlass? = null
    val nestedJavaKlass: JavaKlass.NestedJavaKlass? = null
    val javaKlassAlias: JavaKlassAlias? = null
    val nestedJavaKlassAlias: NestedJavaKlassAlias? = null
    val companionObjekt: Klass.Companion? = null

    val klassInstance = Klass()
    val klassOwnedKlassInstance = Klass.KlassOwnedKlass()
    val objektOwnedKlassInstance = Objekt.ObjektOwnedKlass()
    val klassAliasInstance = KlassAlias()
    val klassOwnedKlassAliasInstance = KlassOwnedKlassAlias()
    val objektOwnedKlassAliasInstance = ObjektOwnedKlassAlias()
    val javaKlassInstance = JavaKlass()
    val nestedJavaKlassInstance = JavaKlass.NestedJavaKlass()
    val javaKlassAliasInstance = JavaKlassAlias()
    val nestedJavaKlassAliasInstance = NestedJavaKlassAlias()

    val klassOwnedObjektInstance = Klass.KlassOwnedObjekt
    val objektInstance = Objekt
    val objektOwnedObjektInstance = Objekt.ObjektOwnedObjekt
    val klassOwnedObjektAliasInstance = KlassOwnedObjektAlias
    val objektAliasInstance = ObjektAlias
    val objektOwnedObjektAliasInstance = ObjektOwnedObjektAlias
    val companionObjektInstance = Klass.Companion

    topLevelFunction()

    Klass().klassFunction()
    Klass.KlassOwnedKlass().klassOwnedKlassFunction()
    Objekt.ObjektOwnedKlass().objektOwnedKlassFunction()
    KlassAlias().klassFunction()
    KlassOwnedKlassAlias().klassOwnedKlassFunction()
    ObjektOwnedKlassAlias().objektOwnedKlassFunction()

    Klass.KlassOwnedObjekt.klassOwnedObjektFunction()
    Objekt.objektFunction()
    Objekt.ObjektOwnedObjekt.objektOwnedObjektFunction()
    KlassOwnedObjektAlias.klassOwnedObjektFunction()
    ObjektAlias.objektFunction()
    ObjektOwnedObjektAlias.objektOwnedObjektFunction()

    JavaKlass().javaKlassFunction()
    JavaKlass.NestedJavaKlass().nestedJavaKlassFunction()
    JavaKlassAlias().javaKlassFunction()
    NestedJavaKlassAlias().nestedJavaKlassFunction()

    JavaKlass.javaKlassStaticFunction()
    JavaKlass.NestedJavaKlass.nestedJavaKlassStaticFunction()
    JavaKlassAlias.javaKlassStaticFunction()
    NestedJavaKlassAlias.nestedJavaKlassStaticFunction()

    Klass.companionObjektFunction()
    Klass.Companion.companionObjektFunction()
    KlassAlias.companionObjektFunction()
}

// MODULE: to(from)

// FILE: toB.kt

fun testB() {
    val klass: Klass? = null
    val klassOwnedKlass: Klass.KlassOwnedKlass? = null
    val klassOwnedObjekt: Klass.KlassOwnedObjekt? = null
    val objekt: Objekt? = null
    val objektOwnedKlass: Objekt.ObjektOwnedKlass? = null
    val objektOwnedObjekt: Objekt.ObjektOwnedObjekt? = null
    val klassAlias: KlassAlias? = null
    val klassOwnedKlassAlias: KlassOwnedKlassAlias? = null
    val klassOwnedObjektAlias: KlassOwnedObjektAlias? = null
    val objektAlias: ObjektAlias? = null
    val objektOwnedKlassAlias: ObjektOwnedKlassAlias? = null
    val objektOwnedObjektAlias: ObjektOwnedObjektAlias? = null
    val javaKlass: JavaKlass? = null
    val nestedJavaKlass: JavaKlass.NestedJavaKlass? = null
    val javaKlassAlias: JavaKlassAlias? = null
    val nestedJavaKlassAlias: NestedJavaKlassAlias? = null
    val companionObjekt: Klass.Companion? = null

    val klassInstance = Klass()
    val klassOwnedKlassInstance = Klass.KlassOwnedKlass()
    val objektOwnedKlassInstance = Objekt.ObjektOwnedKlass()
    val klassAliasInstance = KlassAlias()
    val klassOwnedKlassAliasInstance = KlassOwnedKlassAlias()
    val objektOwnedKlassAliasInstance = ObjektOwnedKlassAlias()
    val javaKlassInstance = JavaKlass()
    val nestedJavaKlassInstance = JavaKlass.NestedJavaKlass()
    val javaKlassAliasInstance = JavaKlassAlias()
    val nestedJavaKlassAliasInstance = NestedJavaKlassAlias()

    val klassOwnedObjektInstance = Klass.KlassOwnedObjekt
    val objektInstance = Objekt
    val objektOwnedObjektInstance = Objekt.ObjektOwnedObjekt
    val klassOwnedObjektAliasInstance = KlassOwnedObjektAlias
    val objektAliasInstance = ObjektAlias
    val objektOwnedObjektAliasInstance = ObjektOwnedObjektAlias
    val companionObjektInstance = Klass.Companion

    topLevelFunction()

    Klass().klassFunction()
    Klass.KlassOwnedKlass().klassOwnedKlassFunction()
    Objekt.ObjektOwnedKlass().objektOwnedKlassFunction()
    KlassAlias().klassFunction()
    KlassOwnedKlassAlias().klassOwnedKlassFunction()
    ObjektOwnedKlassAlias().objektOwnedKlassFunction()

    Klass.KlassOwnedObjekt.klassOwnedObjektFunction()
    Objekt.objektFunction()
    Objekt.ObjektOwnedObjekt.objektOwnedObjektFunction()
    KlassOwnedObjektAlias.klassOwnedObjektFunction()
    ObjektAlias.objektFunction()
    ObjektOwnedObjektAlias.objektOwnedObjektFunction()

    JavaKlass().javaKlassFunction()
    JavaKlass.NestedJavaKlass().nestedJavaKlassFunction()
    JavaKlassAlias().javaKlassFunction()
    NestedJavaKlassAlias().nestedJavaKlassFunction()

    JavaKlass.javaKlassStaticFunction()
    JavaKlass.NestedJavaKlass.nestedJavaKlassStaticFunction()
    JavaKlassAlias.javaKlassStaticFunction()
    NestedJavaKlassAlias.nestedJavaKlassStaticFunction()

    Klass.companionObjektFunction()
    Klass.Companion.companionObjektFunction()
    KlassAlias.companionObjektFunction()
}
