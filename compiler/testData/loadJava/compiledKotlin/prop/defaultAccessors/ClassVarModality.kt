package test

open class ClassVarModality() {
    open var property1 = 1

    final internal var property2 = 1

    open var property3 = 1
      private set

    final internal var property4 = 1
      private  set
}

abstract class ClassVarModalityAbstract {
    abstract var property1 : java.util.Date
      public set
}