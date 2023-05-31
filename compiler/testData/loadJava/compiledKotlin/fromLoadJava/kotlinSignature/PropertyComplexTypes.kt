// TARGET_BACKEND: JVM
package test

import java.util.*

public open class PropertyComplexTypes<T>() {
  public var genericType : T = null!!
  public var listDefinedGeneric : ArrayList<String> = null!!
  public var listGeneric : ArrayList<T> = null!!
  public var listOfGenericList : ArrayList<ArrayList<T>> = null!!
}
