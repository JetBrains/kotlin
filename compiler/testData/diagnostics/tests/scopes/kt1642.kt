// FIR_IDENTICAL
//FILE:a.kt
//KT-1642 kotlin subpackages hide Java's toplevel packages
package a.java

//FILE:b.kt
//+JDK
package a

import java.util.ArrayList