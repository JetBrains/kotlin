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

  public var prop: String = ""

  default object {
    public var prop: Int by TestDelegate({10})
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop
// FLAGS: ACC_PRIVATE

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop$delegate
// FLAGS: ACC_STATIC, ACC_PRIVATE, ACC_FINAL
