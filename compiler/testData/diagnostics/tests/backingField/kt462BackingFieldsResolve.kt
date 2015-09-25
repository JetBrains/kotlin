//KT-462 Consider allowing initializing properties via property names when it is safe
//KT-598 Allow to use backing fields after this expression

package kt462

abstract class TestInitializationWithoutBackingField() {
    val valWithBackingField : Int
    init {
        valWithBackingField = 2
    }

    val valWithoutBackingField : Int
    get() = 42
    init {
        <!VAL_REASSIGNMENT!>valWithoutBackingField<!> = 45
    }

    var finalDefaultVar : Int
    init {
        finalDefaultVar = 3
    }

    open var openVar : Int
    init {
        <!INITIALIZATION_USING_BACKING_FIELD_OPEN_SETTER!>openVar<!> = 4
    }

    var varWithCustomSetter : Int
    set(v: Int) {
        field = v
    }
    init {
        <!INITIALIZATION_USING_BACKING_FIELD_CUSTOM_SETTER!>varWithCustomSetter<!> = 3
    }

    var varWithoutBackingField : Int
    get() = 3
    set(v: Int) {}
    init {
        varWithoutBackingField = 4
    }

    abstract var abstractVar : Int
    init {
        abstractVar = 34
    }
}

abstract class TestInitializationThroughBackingField() {
    val valWithBackingField : Int
    init {
        <!BACKING_FIELD_USAGE_DEPRECATED!>$valWithBackingField<!> = 2
    }

    val valWithoutBackingField : Int
    get() = 42
    init {
        <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$valWithoutBackingField<!> = 45
    }

    var finalDefaultVar : Int
    init {
        <!BACKING_FIELD_USAGE_DEPRECATED!>$finalDefaultVar<!> = 3
    }

    open var openVar : Int
    init {
        <!BACKING_FIELD_USAGE_DEPRECATED!>$openVar<!> = 4
    }

    var varWithCustomSetter : Int
    set(v: Int) {
        field = v
    }
    init {
        <!BACKING_FIELD_USAGE_DEPRECATED!>$varWithCustomSetter<!> = 3
    }

    var varWithoutBackingField : Int
    get() = 3
    set(v: Int) {}
    init {
        <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$varWithoutBackingField<!> = 4
    }

    abstract var abstractVar : Int
    init {
        <!NO_BACKING_FIELD_ABSTRACT_PROPERTY!>$abstractVar<!> = 34
    }
}

class TestBackingFieldsVisibility() {
    var a : Int = 712
    init {
        <!BACKING_FIELD_USAGE_DEPRECATED!>$a<!> = 37
        this.$a = 357
    }

    fun foo() {
        <!BACKING_FIELD_USAGE_DEPRECATED!>$a<!> = 334
        this.$a = 347
    }

    fun foo(a: TestBackingFieldsVisibility) {
        val <!UNUSED_VARIABLE!>b<!> : Int = 3
        <!UNRESOLVED_REFERENCE!>$b<!> = 342
        <!INACCESSIBLE_BACKING_FIELD!>a.$a<!> = 3
    }

    val x = <!INACCESSIBLE_BACKING_FIELD!>$topLevelVar<!>

    inner class Inner() {
        val z = this@TestBackingFieldsVisibility.<!BACKING_FIELD_USAGE_DEPRECATED!>$x<!>
    }

    <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS!>abstract<!> val w = 11
    get() = field //test there is no second error here
}

val topLevelVar = 11

class T() {
    val z : Int get() = 42

    init {
        <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>this.$z<!> = 34
    }

    fun foo() {
        <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>this.$z<!> = 343
    }

    val a =  object {
        val x = <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$z<!>
        init {
            <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$z<!> = 23
        }
    }

    var x: Int = 2
    get() {
        val <!UNUSED_VARIABLE!>o<!> = object {
            init {
                field = 34
            }
            fun foo() {
                field = 23
            }
        }
        return 1
    }

    var r: Int = <!BACKING_FIELD_USAGE_DEPRECATED!>$x<!>
    set(v: Int) {
        if (true) {
            field = 33
        }
        else {
            field = 35
        }
    }

    fun bar() {
        <!BACKING_FIELD_USAGE_DEPRECATED!>$x<!> = 34
        val <!UNUSED_VARIABLE!>o<!> = object {
            val y = <!BACKING_FIELD_USAGE_DEPRECATED!>$x<!>
            init {
                <!BACKING_FIELD_USAGE_DEPRECATED!>$x<!> = 422
            }
        }
    }
}
