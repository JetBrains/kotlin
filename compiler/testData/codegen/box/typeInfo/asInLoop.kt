// TARGET_BACKEND: JVM

import java.io.*

fun foo(args: Array<String>) {
  val reader = BufferedReader(InputStreamReader(System.`in`))
  while(true) {
    val cmd = reader.readLine() as String
  }
}

fun box() = "OK"
