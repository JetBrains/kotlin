// FILE: staticTraitProperty.java
package test;

class staticTraitProperty {

    public static void main(String[] args) {
        int i = Test.valProp;
    }
}

// FILE: staticTraitProperty.kt
package test

interface Test {

  companion object {
    public const val valProp: Int = 10
  }

}
