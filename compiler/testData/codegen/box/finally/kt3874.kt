// IGNORE_BACKEND_FIR: JVM_IR
fun test1(): String {
    var r = ""
    for (i in 1..2)  {
        try {
            r += "O"
            continue
        } finally {
            r += "K"
            break
        }
    }
    return r
}

fun test2(): String {
    var r = ""
    for (i in 1..2)  {
        try {
            r += "O"
            break
        } finally {
            r += "K"
            continue
        }
    }
    return r
}

fun box(): String {
  if (test1() != "OK") return "fail1: ${test1()}"

  if (test2() != "OKOK") return "fail2: ${test2()}"

  return "OK"
}