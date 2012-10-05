package test

import java.util.*

public open class PropertyArrayTypes<T>(p0 : T) : java.lang.Object() {
  public var arrayOfArrays : Array<Array<String>> = Array<Array<String>>(0, { Array<String>(0, { "" })})
  public var array : Array<String> = Array<String>(0, { "" })
  public var genericArray : Array<T> = Array<T>(0, { p0 })
}
