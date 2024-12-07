// TARGET_BACKEND: JVM
package test

open class ClassVarModality() {
    open var property1: Int = 1

    final internal var property2: Int = 1

    open var property3: Int = 1

    final internal var property4: Int = 1
      private  set
}

abstract class ClassVarModalityAbstract {
    abstract var property1 : java.util.Date
      public set
}
