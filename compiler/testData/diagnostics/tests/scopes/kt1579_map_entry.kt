//FILE:a.kt
//+JDK
package a
import java.util.Map.*

fun foo(b : Entry<String, String>) = b

//FILE:b.kt
//+JDK
package b

import java.util.Map.Entry
fun bar(b : Entry<String, String>) = b

//FILE:c.kt
//+JDK
package c

import java.util.Map
fun fff(b: Map.Entry<String, String>) = b