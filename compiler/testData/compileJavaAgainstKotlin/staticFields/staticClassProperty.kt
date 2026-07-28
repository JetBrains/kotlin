// FILE: staticClassProperty.java
package test;

class staticClassProperty {

    public static void main(String[] args) {
        int i = Test.valProp;
        int j = Test.varProp;
        Test.varProp = 100;
    }
}

// FILE: staticClassProperty.kt
package test

class Test {

  companion object {
    public const val valProp: Int = 10

    @JvmField public var varProp: Int = 10
  }

}
