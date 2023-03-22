// IGNORE_REVERSED_RESOLVE
// !DIAGNOSTICS: +UNUSED_VALUE, +UNUSED_CHANGED_VALUE, +UNUSED_PARAMETER, +UNUSED_VARIABLE

package unused_variables

fun testSimpleCases() {
    var i = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>2<!>
    <!UNUSED_VALUE!>i =<!> 34
    i = 34
    doSmth(i)
    <!UNUSED_VALUE!>i =<!> 5

    var j = 2
    j = <!UNUSED_CHANGED_VALUE!>j++<!>
    <!UNUSED_VALUE!>j =<!> <!UNUSED_CHANGED_VALUE!>j--<!>
}

class IncDec() {
  operator fun inc() : IncDec = this
  operator fun dec() : IncDec = this
}

class MyTest() {
    fun testIncDec() {
      var x = IncDec()
      x++
      ++x
      x--
      --x
      x = <!UNUSED_CHANGED_VALUE!>x++<!>
      x = <!UNUSED_CHANGED_VALUE!>x--<!>
      x = ++x
      <!UNUSED_VALUE!>x =<!> --x
    }

    var a: String = "s"
        set(v: String) {
            var i: Int = 23
            doSmth(i)
            <!UNUSED_VALUE!>i =<!> 34
            field = v
        }

    init {
        a = "rr"
    }

    fun testSimple() {
        a = "rro"

        var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>i<!> = 1;
        <!UNUSED_VALUE!>i =<!> 34;
        <!UNUSED_VALUE!>i =<!> 456;
    }

    fun testWhile() {
        var a : Any? = true
        var b : Any? = 34
        while (a is Any) {
            a = null
        }
        while (b != null) {
            <!UNUSED_VALUE!>a =<!> null
        }
    }

    fun testIf() {
        var a : Any
        if (1 < 2) {
            a = 23
        }
        else {
            a = "ss"
            doSmth(<!DEBUG_INFO_SMARTCAST!>a<!>)
        }
        doSmth(a)

        if (1 < 2) {
            <!UNUSED_VALUE!>a =<!> 23
        }
        else {
            <!UNUSED_VALUE!>a =<!> "ss"
        }
    }

    fun testFor() {
        for (i in 1..10) {
            doSmth(i)
        }
    }

    fun doSmth(<!UNUSED_PARAMETER!>s<!>: String) {}
    fun doSmth(<!UNUSED_PARAMETER!>a<!>: Any) {}
}

fun testInnerFunctions() {
    var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>y<!> = 1
    fun foo() {
        y = 1
    }
    var z = 1
    fun bar() {
        doSmth(z)
    }
}

fun testFunctionLiterals() {
    var x = 1
    var <!UNUSED_VARIABLE!>fl<!> = {
        x
    }
    var y = 2
    var <!UNUSED_VARIABLE!>fl1<!> = {
        doSmth(y)
    }
}

interface Trait {
    fun foo()
}

fun testObject() : Trait {
    val x = 24
    val o = object : Trait {
        val y : Int   //in this case y should not be marked as unused
           get() = 55

        override fun foo() {
            doSmth(x)
        }
    }

    return o
}

fun doSmth(<!UNUSED_PARAMETER!>i<!> : Int) {}
