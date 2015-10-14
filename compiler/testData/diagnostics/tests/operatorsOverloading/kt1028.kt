//KT-1028 Wrong type checking for plusAssign
package kt1028

import java.util.*

class event<T>()
{
    val callbacks = ArrayList< Function1<T, Unit> >() // Should be ArrayList<()->Unit>, bug posted

    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun plusAssign(f : (T) -> Unit) = callbacks.add(f)
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun minusAssign(f : (T) -> Unit) = callbacks.remove(f)
    fun call(value : T) { for(c in callbacks) c(value) }
}

class MouseMovedEventArgs()
{
    public val X : Int = 0
}

class Control()
{
    public val MouseMoved : event<MouseMovedEventArgs> = event<MouseMovedEventArgs>()

    fun MoveMouse() = MouseMoved.call(MouseMovedEventArgs())
}

class Test()
{
    fun test()
    {
        val control = Control()
        control.MouseMoved <!ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT!>+=<!> { it.X } // here
        control.MouseMoved.plusAssign( { it.X } ) // ok
    }
}