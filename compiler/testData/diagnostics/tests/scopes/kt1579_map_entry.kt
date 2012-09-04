//FILE:a.kt
//+JDK
package a
import jet.Map.*

fun foo(b : Entry<String, String>) = b

//FILE:b.kt
//+JDK
package b

import jet.Map.Entry
fun bar(b : Entry<String, String>) = b

//FILE:c.kt
//+JDK
package c

fun fff(b: Map.Entry<String, String>) = b