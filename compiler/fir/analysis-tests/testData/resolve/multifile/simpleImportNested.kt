// FILE: A.kt

package a

class MyClass {
    open class MyNested
}

// FILE: B.kt

package b

import a.MyClass.MyNested

class YourClass : MyNested()