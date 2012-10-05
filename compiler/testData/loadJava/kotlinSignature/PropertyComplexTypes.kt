package test

import java.util.*

public open class PropertyComplexTypes<T>(p0 : T) : java.lang.Object() {
  public var genericType : T = p0
  public var listDefinedGeneric : ArrayList<String> = ArrayList<String>()
  public var listGeneric : ArrayList<T> = ArrayList<T>()
  public var listOfGenericList : ArrayList<ArrayList<T>> = ArrayList<ArrayList<T>>()
}
