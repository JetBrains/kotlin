package test

import java.util.*

public open class PropertyArrayTypes<T>() : java.lang.Object() {
  public var arrayOfArrays : Array<out Array<out String>> = null!!
  public var array : Array<out String> = null!!
  public var genericArray : Array<out T> = null!!
}
