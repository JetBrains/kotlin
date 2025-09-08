// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// ISSUE: KT-80600
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object DummyDelegate : ReadWriteProperty<Foo, String> {
    override fun getValue(thisRef: Foo, property: KProperty<*>): String { return "" }
    override fun setValue(thisRef: Foo, property: KProperty<*>, value: String) {}
}

open class Foo {
    open var foo: String by DummyDelegate
        <!GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY!>private<!> get
        <!PRIVATE_SETTER_FOR_OPEN_PROPERTY!>private<!> <!WRONG_MODIFIER_TARGET!>final<!> set
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, objectDeclaration, override, propertyDeclaration,
propertyDelegate, setter, starProjection, stringLiteral */
