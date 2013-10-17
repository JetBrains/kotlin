TailRecursive fun a(counter : Int, text : String, e : Int, x : jet.Any) : Int {
    if (counter == 1000000) {
        return counter
    } else if (counter == 50) {
        return a(counter + 1, text, e, "no tail 5") + 1 // no tail recursion here
    } else {
        return a(counter + 1, text, e + 1, "tail 7") // tail here
    }
}

TailRecursive fun a2(counter : Int, x : Any) : Int {
    if (counter == 0) {
        return 0
    } else if (counter == 100) {
        a2(counter - 1, "no tail 15") // no tail here!
    }
    return a2(counter - 1, "tail 17") // tail here
}

TailRecursive fun b(acounter : Int, x : Any) : Unit {
    if (acounter == 50) {
        b(acounter + 1, "tail 22") // no tail recursion here
        dummy()
    } else if (acounter == 70) {
        b(acounter + 1, "tail 25") // tail here
        return
    } else if (acounter == 75) {
        if (acounter > 10) {
            b(acounter + 1, "tail 29") // tail here!
        }
        return
    } else if (acounter == 80) {
        var j = 0
        while (j < 1) {
            j = j + 1
            b(acounter + 1, "no tail 36") // no tail here
        }
        return
    } else if (acounter < 1000000) {
        b(acounter + 1, "tail 40")   // tail here
    }
}

TailRecursive fun c(counter : Int, x : Any) : Int = if (counter < 1000000) c(counter + 1, "tail 44") else counter

TailRecursive fun d(counter : Int, x : Any) : Unit {
    if (counter < 1000000) {
        d(counter + 1, "tail 48")
    } else {
        dummy()
    }
}

TailRecursive fun e(counter : Int, x : Any) : Unit {
    if (counter > 0) {
        try {
            dummy()
        } finally {
            e(counter - 1, "tail 59")
        }
    }
}

TailRecursive fun e2(counter : Int, a : Any) {
    if (counter > 0) {
        try {
            dummy()
        } finally {
            try {
                throw IllegalStateException()
            } catch (e : Exception) {
                e2(counter - 1, "tail 69")
            }
        }
    }
}

TailRecursive fun e3(counter : Int, x : Any) {
    if (counter > 0) {
        try {
            throw IllegalStateException()
        } catch (e : Exception) {
            e3(counter - 1, "no tail 80")
        } finally {
            dummy()
        }
    }
}

TailRecursive fun g(counter : Int, x : Any) {
    val z = { g(counter - 1, "no tail 91") }
    z()
}

TailRecursive fun g2() {
    [TailRecursive] fun g3(counter : Int, x : Any) {
        if (counter > 0) { g3(counter - 1, "tail 97") }
    }
    g3(1000000, "test")
}

class A {
    TailRecursive fun f1(x : Any) {
        this.f1("tail 104")
    }

    TailRecursive fun f2(x : Any) {
        f2("tail 108")
    }

    TailRecursive fun f3(a : A, x : Any) {
        a.f3(a, "no tail 112") // non-tail recursion, could be potentially resolved by condition if (a == this) f3() else a.f3()
    }
}

class B {
    inner class C {
        TailRecursive fun h(counter : Int, x : Any) {
            if (counter > 0) { this@C.h(counter - 1, "tail 119") }
        }

        TailRecursive fun h2(x : Any) {
            this@B.h2("no tail no recursion 123") // keep vigilance
        }

    }

    fun makeC() : C = C()

    fun h2(x : Any) {
    }
}

TailRecursive fun String.repeat(num : Int, acc : StringBuilder = StringBuilder()) : String =
        if (num == 0) acc.toString()
        else repeat(num - 1, acc.append(this)!!) // tail

fun escapeChar(c : Char) : String? = when (c) {
    '\\' -> "\\\\"
    '\n' -> "\\n"
    '"'  -> "\\\""
    else -> "" + c
}

TailRecursive fun String.escape(i : Int = 0, result : String = "") : String =
        if (i == length) result
        else escape(i + 1, result + escapeChar(get(i)))

TailRecursive fun <T, A> Iterator<T>.foldl(acc : A, foldFunction : (e : T, acc : A) -> A) : A =
        if (!hasNext()) acc
        else foldl(foldFunction(next(), acc), foldFunction)

TailRecursive fun withWhen(counter : Int, x : Any) : Int =
    when (counter) {
        0 -> counter
        50 -> 1 + withWhen(counter - 1, "no tail")
        else -> withWhen(counter - 1, "tail")
    }

TailRecursive fun withWhen2(counter : Int, x : Any) : Int =
  when {
    counter == 0 -> counter
    counter == 50 -> 1 + withWhen2(counter - 1, "no tail")
    withWhen2(counter - 1, "no tail") == 1 -> 10
    else -> 1
  }

fun tailButNoAnnotation(x : Any) {
  tailButNoAnnotation(x)
}

fun dummy() : Unit {
}

fun box() : String {
    if (1000000 + 1 != a(1, "test", 0, 1)) {
        return "FAIL"
    }
    a2(100000, "test")
    b(1, "test")
    c(1, "test")
    d(1, "test")
    e(1000000, "test")
    e2(1000000, "test")
    g2()
    B().makeC().h(1000000, "test")
    if (!"abrakadabra".repeat(5).equals("abrakadabraabrakadabraabrakadabraabrakadabraabrakadabra")) {
        return "FAIL: repeat = ${"abrakadabra".repeat(5)}"
    }

    "test me not \\".escape()

    val sum = (1..1000000).iterator().foldl(0) { (e : Int, acc : Long) ->
        acc + e
    }

    if (sum != 500000500000) {
        return "FAIL: foldl = ${sum}"
    }

    if (withWhen(100000, "test") != 1) {
        return "FAIL: withWhen != 1"
    }

    return "OK"
}
