import kotlin.reflect.KProperty

public open class TestDelegate<T: Any>(private val initializer: () -> T) {
    private var value: T? = null

    operator open fun getValue(thisRef: Any?, desc: KProperty<*>): T {
        if (value == null) {
            value = initializer()
        }
        return value!!
    }

    operator open fun setValue(thisRef: Any?, desc: KProperty<*>, svalue : T) {
        value = svalue
    }
}

class Test {

  public var prop: String = ""

  companion object {
    public var prop: Int by TestDelegate({10})
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop
// FLAGS: ACC_PRIVATE

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop$delegate
// FLAGS: ACC_STATIC, ACC_PRIVATE, ACC_FINAL
