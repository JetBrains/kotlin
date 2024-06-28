import kotlin.reflect.KProperty

class Foo(a: Int, val b:Foo, var c:Boolean, private val d: List, protected val e: Long = 2) {
  val f1 = 2

  val intConst: dynamic = 30
  val arrayConst: Any = byteArrayOf(1,2)

  protected var f2 = 3

  var name: String = "x"

  val isEmpty get() = false
  var isEmptyMutable: Boolean?
  var islowercase: Boolean?
  var isEmptyInt: Int?
  var getInt: Int?
  private var noAccessors: String

  internal var stringRepresentation: String
    get() = this.toString()
    set(value) {
        setDataFromString(value)
    }

  const val SUBSYSTEM_DEPRECATED: String = "This subsystem is deprecated"

  const val CONSTANT_WITH_ESCAPES = "A\tB\nC\rD\'E\"F\\G\$H"

  var counter = 0
    set(value) {
        if (value >= 0) field = value
    }
  var counter2 : Int?
    get() = field
    set(value) {
        if (value >= 0) field = value
    }

  lateinit var subject: Unresolved
  internal lateinit var internalVarPrivateSet: String
    private set
  protected lateinit var protectedLateinitVar: String

  var delegatedProp: String by Delegate()
  var delegatedProp2 by MyProperty()
  private var privateDelegated: Int by Delegate()
  var lazyProp: String by lazy { "abc" }

  val Int.intProp: Int
    get() = 1

  final internal var internalWithPrivateSet: Int = 1
    private  set

  protected var protectedWithPrivateSet: String = ""
    private set

  private var privateVarWithPrivateSet = { 0 }()
    private set

  private val privateValWithGet: String?
    get() = ""

  private var privateVarWithGet: Object = Object()
    get

  val sum: (Int)->Int = { x: Int -> sum(x - 1) + x }

  companion object {
    public val prop3: Int = { 12 }()
      get() {
        return field
      }
    public var prop7 : Int = { 20 }()
      set(i: Int) {
        field++
      }
    private const val contextBean = Context.BEAN

    val f1 = 4
  }
}

class MyProperty<T> {
    operator fun getValue(t: T, p: KProperty<*>): Int = 42
    operator fun setValue(t: T, p: KProperty<*>, i: Int) {}
}

class Modifiers {
  @delegate:Transient
  val plainField: Int = 1
}

interface A {
  public var int1: Int
    private set
    protected get
  public var int2: Int
    public get
    internal set
}

class Foo2 {
  val foo get() = getMeNonNullFoo()
  val foo2: Foo get() = getMeNonNullFoo()
  fun getMeNonNullFoo() : Foo = Foo()
}

// COMPILATION_ERRORS
