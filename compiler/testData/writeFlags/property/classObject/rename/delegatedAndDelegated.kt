public open class TestDelegate<T: Any>(private val initializer: () -> T) {
    private var value: T? = null

    public open fun get(thisRef: Any?, desc: PropertyMetadata): T {
        if (value == null) {
            value = initializer()
        }
        return value!!
    }

    public open fun set(thisRef: Any?, desc: PropertyMetadata, svalue : T) {
        value = svalue
    }
}

class Test {

  public val prop: Int by TestDelegate({10})

  default object {
    public var prop: Int by TestDelegate({10})
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop$delegate$1
// FLAGS: ACC_PRIVATE, ACC_FINAL

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop$delegate
// FLAGS: ACC_STATIC, ACC_PRIVATE, ACC_FINAL