
import kotlin.reflect.KProperty

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

internal var stringRepresentation: String
get() = this.toString()
set(value) {
	setDataFromString(value)
}

const val SUBSYSTEM_DEPRECATED: String = "This subsystem is deprecated"

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

var lazyProp: String by lazy { "abc" }

val Int.intProp: Int
get() = 1

final internal var internalWithPrivateSet: Int = 1
private  set

protected var protectedWithPrivateSet: String = ""
private set

val sum: (Int)->Int = { x: Int -> sum(x - 1) + x }

operator fun getValue(t: T, p: KProperty<*>): Int = 42
operator fun setValue(t: T, p: KProperty<*>, i: Int) {}

@delegate:Transient
val plainField: Int = 1

@delegate:Transient
val lazy by lazy { 1 }

public var int1: Int
	private set
	protected get
public var int2: Int
	public get
	internal set

private val privateVal: Int = 42
private val privateVar: Int = 42
private fun privateFun(): Int = 42
val x: String = ""
	get;
var x: String = ""
	private set;
