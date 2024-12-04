// TARGET_BACKEND: JVM
package test

class ClassVal() {
    var property1: Int = 1
      set

    var property2: Object = Object()
      protected set

    var property3: Object = Object()
      private set

    protected var property4: String = ""
      set

    protected var property5: String = ""
      private set

    public var property8: Int = 1
      set

    public var property9: Int = 1
      private set

    public var property10: Int = 1
      protected set

    public var property11: Int = 1
      internal set
}
