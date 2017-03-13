// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

import java.io.*

fun foo(args: Array<String>) {
  val reader = BufferedReader(InputStreamReader(System.`in`))
  while(true) {
    val cmd = reader.readLine() as String
  }
}

fun box() = "OK"
