//KT-462 Consider allowing initializing properties via property names when it is safe
//KT-598 Allow to use backing fields after this expression

package kt462

abstract class TestInitializationWithoutBackingField() {
    val valWithBackingField : Int
    {
        valWithBackingField = 2
    }

    val valWithoutBackingField : Int
    get() = 42
    {
        <!VAL_REASSIGNMENT!>valWithoutBackingField<!> = 45
    }

    var finalDefaultVar : Int
    {
        finalDefaultVar = 3
    }

    open var openVar : Int
    {
        <!INITIALIZATION_USING_BACKING_FIELD_OPEN_SETTER!>openVar<!> = 4
    }

    var varWithCustomSetter : Int
    set(v: Int) {
        $varWithCustomSetter = v
    }
    {
        <!INITIALIZATION_USING_BACKING_FIELD_CUSTOM_SETTER!>varWithCustomSetter<!> = 3
    }

    var varWithoutBackingField : Int
    get() = 3
    set(v: Int) {}
    {
        varWithoutBackingField = 4
    }

    abstract var abstractVar : Int
    {
        abstractVar = 34
    }
}

abstract class TestInitializationThroughBackingField() {
    val valWithBackingField : Int
    {
        $valWithBackingField = 2
    }

    val valWithoutBackingField : Int
    get() = 42
    {
        <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$valWithoutBackingField<!> = 45
    }

    var finalDefaultVar : Int
    {
        $finalDefaultVar = 3
    }

    open var openVar : Int
    {
        $openVar = 4
    }

    var varWithCustomSetter : Int
    set(v: Int) {
        $varWithCustomSetter = v
    }
    {
        $varWithCustomSetter = 3
    }

    var varWithoutBackingField : Int
    get() = 3
    set(v: Int) {}
    {
        <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$varWithoutBackingField<!> = 4
    }

    abstract var abstractVar : Int
    {
        <!NO_BACKING_FIELD_ABSTRACT_PROPERTY!>$abstractVar<!> = 34
    }
}

class TestBackingFieldsVisibility() {
    var a : Int = 712
    {
        $a = 37
        this.$a = 357
    }

    fun foo() {
        $a = 334
        this.$a = 347
    }

    fun foo(a: TestBackingFieldsVisibility) {
        val <!UNUSED_VARIABLE!>b<!> : Int = 3
        <!UNRESOLVED_REFERENCE!>$b<!> = 342
        <!INACCESSIBLE_BACKING_FIELD!>a.$a<!> = 3
    }

    val x = <!INACCESSIBLE_BACKING_FIELD!>$namespaceLevelVar<!>

    inner class Inner() {
        val z = this@TestBackingFieldsVisibility.$x
    }

    <!ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS!>abstract<!> val w = 11
    get() = $w //test there is no second error here
}

val namespaceLevelVar = 11

class T() {
    val z : Int get() = 42

    {
        <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>this.$z<!> = 34
    }

    fun foo() {
        <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>this.$z<!> = 343
    }

    val a =  object {
        val x = <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$z<!>
        {
            <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$z<!> = 23
        }
    }

    var x: Int = 2
    get() {
        val <!UNUSED_VARIABLE!>o<!> = object {
            {
                $x = 34
            }
            fun foo() {
                $x = 23
            }
        }
        return 1
    }

    var r: Int = $x
    set(v: Int) {
        if (true) {
            $r = 33
        }
        else {
            $r = 35
        }
    }

    fun bar() {
        $x = 34
        val <!UNUSED_VARIABLE!>o<!> = object {
            val y = $x
            {
                $x = 422
            }
        }
    }
}
