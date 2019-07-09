// FILE: Definitions.kt
// IR_FILE: kt29833.txt
package interop

object Definitions {
    const val KT_CONSTANT = Interface.CONSTANT

    val ktValue = Interface.CONSTANT
}

// FILE: interop/Interface.java
package interop;

class Interface {
    public static final String CONSTANT = "constant";
    public CharSequence chars;
}
