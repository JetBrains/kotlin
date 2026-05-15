// TARGET_BACKEND: JVM
// FULL_JDK
// IGNORE_BACKEND: JKLIB
// ^KT-86348 java.lang.AssertionError: Can't find built-in class kotlin.Cloneable

import java.util.ArrayList

data class Some<T>(val value: T)

interface MyList<T> : List<Some<T>>

open class SomeList<T> : MyList<T>, ArrayList<Some<T>>()

class FinalList : SomeList<String>()
