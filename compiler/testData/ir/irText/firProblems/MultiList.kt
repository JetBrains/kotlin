// FULL_JDK

import java.util.ArrayList

data class Some<T>(val value: T)

interface MyList<T> : List<Some<T>>

open class SomeList<T> : MyList<T>, ArrayList<Some<T>>()

class FinalList : SomeList<String>()
