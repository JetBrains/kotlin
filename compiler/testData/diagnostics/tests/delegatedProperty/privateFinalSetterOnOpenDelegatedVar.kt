// RUN_PIPELINE_TILL: BACKEND
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
        <!PRIVATE_SETTER_FOR_OPEN_PROPERTY!>private<!> <!WRONG_MODIFIER_TARGET!>final<!> set
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, objectDeclaration, override, propertyDeclaration,
propertyDelegate, setter, starProjection, stringLiteral */
